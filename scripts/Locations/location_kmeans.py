from collections import Counter
from math import log, e
import os
import pandas as pd
import numpy as np
import ast
import sys
from sklearn.cluster import MiniBatchKMeans
import traceback


# apply on full given dataset returns label list
def apply_mini_batch_kmeans(dataset, n_clusters, count, weights):
    global total_cluster_count
    mbk = MiniBatchKMeans(n_clusters=n_clusters, init_size=3 * n_clusters, n_init=10)

    labels = mbk.fit_predict(dataset, sample_weight=weights)
    centers = mbk.cluster_centers_  # Coordinates of cluster centers.
    label_counts = dict(Counter(labels))

    return labels, centers, label_counts


def entropy(label_count, base=None):
    n_labels = len(label_count.keys())

    if n_labels <= 1:
        return 0

    counts = list(label_count.values())
    probs = [count / n_labels for count in counts]

    ent = 0.

    # Compute entropy
    base = e if base is None else base
    for i in probs:
        ent -= i * log(i, base)
    return ent


def find_best_mini_batch_k_means(ds, am_of_cl, am_of_itrs, weights=None):
    best_labels, best_centers, best_label_count = apply_mini_batch_kmeans(ds, am_of_cl, am_of_itrs, weights)

    best_entropy = entropy(best_label_count)

    for i in range(am_of_itrs - 1):
        labels, centers, label_count = apply_mini_batch_kmeans(ds, am_of_cl, 1, weights)
        ent = entropy(label_count)

        if ent > best_entropy:
            best_labels = labels
            best_centers = centers
            best_label_count = label_count

    return best_labels, best_centers, best_label_count


def get_new_td_cluster_amounts(label_counts, total_records, preferred_cluster_amount):
    new_cluster_amounts = {}
    new_cluster_sizes = {}
    for label, count in label_counts.items():
        cluster_amount = round((count / total_records) * preferred_cluster_amount)
        if cluster_amount == 0:
            cluster_amount = 1
        new_cluster_amounts[label] = cluster_amount
        new_cluster_sizes[label] = count / cluster_amount

    while sum(new_cluster_amounts.values()) != preferred_cluster_amount:
        current_cluster_amount = sum(new_cluster_amounts.values())
        if current_cluster_amount < preferred_cluster_amount:
            max_subcluster_label = None
            max_subcluster_size = -1
            for label, cluster_size in new_cluster_sizes.items():
                if cluster_size > max_subcluster_size:
                    max_subcluster_size = cluster_size
                    max_subcluster_label = label

            new_cluster_amounts[max_subcluster_label] += 1
            new_cluster_sizes[max_subcluster_label] = label_counts[max_subcluster_label] / new_cluster_amounts[
                max_subcluster_label]
        else:
            min_subcluster_label = None
            min_subcluster_size = total_records
            for label, cluster_size in new_cluster_sizes.items():
                if cluster_size < min_subcluster_size:
                    if new_cluster_amounts[label] > 1:
                        min_subcluster_size = cluster_size
                        min_subcluster_label = label

            new_cluster_amounts[min_subcluster_label] -= 1
            new_cluster_sizes[min_subcluster_label] += label_counts[min_subcluster_label] / new_cluster_amounts[
                min_subcluster_label]

    return new_cluster_amounts


def top_down_clustering(dataset, best_k_means_try, preferred_cluster_amounts):
    total_records = len(dataset)
    centers_dict = {}
    label_counts_dict = {}
    label_dict = {}
    dataset_np = dataset.values

    for i, cluster_amount in enumerate(preferred_cluster_amounts):
        #print('Amount of clusters\t' + str(cluster_amount))
        if i == 0:
            labels, centers, label_counts = find_best_mini_batch_k_means(dataset, cluster_amount, best_k_means_try)

            label_dict[i] = {str(a): b for a, b in
                             zip(dataset[['latitude', 'longitude']].values.tolist(), labels)}
            centers_dict[i] = centers.tolist()
            label_counts_dict[i] = label_counts

        else:
            current_centers = []
            current_label_counts = {}
            current_label_dict = {}

            # recluster each previus cluster

            # put prev clusters in dict for fast access
            prev_clusters = {}

            def get_prev_cluster_dict(row):
                prev_label = label_dict[i - 1][str(row.tolist())]
                if prev_label not in prev_clusters:
                    tmp = np.empty((0, 2), np.float64)
                    prev_clusters[prev_label] = np.append(tmp, [row], axis=0)
                else:
                    prev_clusters[prev_label] = np.append(prev_clusters[prev_label], [row], axis=0)

            np.apply_along_axis(get_prev_cluster_dict, 1, dataset_np)

            current_label = 0

            new_cluster_amounts = get_new_td_cluster_amounts(label_counts_dict[i - 1], total_records,
                                                             preferred_cluster_amounts[i])

            for label, amount in new_cluster_amounts.items():

                current_dataset = prev_clusters[label]
                if amount <= 1:
                    tmp = {str(a): b for a, b in
                           zip(current_dataset.tolist(),
                               [current_label] * len(current_dataset))}

                    current_label_dict.update(tmp)
                    current_centers.append(centers_dict[i - 1][label])
                    current_label_counts[current_label] = label_counts_dict[i - 1][label]
                    current_label += 1
                else:
                    labels, centers, label_counts = find_best_mini_batch_k_means(current_dataset, amount,
                                                                                 best_k_means_try)

                    # change labels using current_label
                    labels = [label + current_label for label in labels]
                    label_counts = {key + current_label: value for key, value in label_counts.items()}

                    # add new labels to dataset
                    current_label_dict.update({str(a): b for a, b in zip(
                        current_dataset.tolist(), labels)})
                    current_centers = current_centers + centers.tolist()
                    current_label_counts.update(label_counts)
                    current_label += amount

            label_dict[i] = current_label_dict
            centers_dict[i] = current_centers
            label_counts_dict[i] = current_label_counts
    return label_dict, centers_dict, label_counts_dict


def bottom_up_clustering(dataset, best_k_means_try, preferred_cluster_amounts):
    preferred_cluster_amounts = reversed(preferred_cluster_amounts)
    total_records = len(dataset)
    centers_dict = {}
    label_counts_dict = {}
    label_dict = {}

    for i, cluster_amount in enumerate(preferred_cluster_amounts):
        #print('Cluster size\t' + str(cluster_amount))
        if i == 0:
            labels, centers, label_counts = find_best_mini_batch_k_means(dataset, cluster_amount, best_k_means_try)
            label_dict[i] = {str(a): b for a, b in
                             zip(dataset[['latitude', 'longitude']].values.tolist(), labels)}
            centers_dict[i] = centers.tolist()
            label_counts_dict[i] = label_counts
        else:
            label_counts = dict(Counter(label_dict[i - 1].values()))
            weights = [0 for i in range(len(centers_dict[i - 1]))]
            for index, center in enumerate(centers_dict[i - 1]):
                if index in label_counts:
                    weights[index] = label_counts[index]

            labels, centers, label_counts = find_best_mini_batch_k_means(centers_dict[i - 1], cluster_amount,
                                                                         best_k_means_try, weights)

            tmp = {}

            def get_centers(row):
                row = str(row.tolist())
                index = label_dict[i - 1][row]
                tmp[row] = labels[index]

            np.apply_along_axis(get_centers, 1, dataset[['latitude', 'longitude']].values)

            label_dict[i] = tmp
            centers_dict[i] = centers.tolist()
            label_counts_dict[i] = label_counts

    return label_dict, centers_dict, label_counts_dict

def get_hierarchy_top_down(dataset, label_dict, centers_dict, label_counts_dict, col_separator):
    h = pd.DataFrame(columns=range(len(label_dict)+2))
    for index, row in dataset.iterrows():
        lat_general = format_string.format(row['latitude'])
        lon_general = format_string.format(row['longitude'])
        coordinates_general = lat_general + col_separator + lon_general
        output_row = [coordinates_general]
        dictionary_key = str(list(row))
        for key in reversed(list(label_dict.keys())):
            center = centers_dict[key][label_dict[key][dictionary_key]]
            lat_center = format_string.format(center[0])
            lon_center = format_string.format(center[1])
            center_str = lat_center + col_separator + lon_center
            output_row.append(center_str)
        output_row.append('*')
        h.loc[len(h)] = output_row
    return h



def get_hierarchy_bottom_up(path, dataset, label_dict, centers_dict, label_counts_dict, col_separator):
    h = pd.DataFrame(columns=range(len(label_dict)+2))
    for index, row in dataset.iterrows():
        lat_general = format_string.format(row['latitude'])
        lon_general = format_string.format(row['longitude'])
        coordinates_general = lat_general + col_separator + lon_general
        output_row = [coordinates_general]
        dictionary_key = str(list(row))
        for key in label_dict.keys():
            center = centers_dict[key][label_dict[key][dictionary_key]]
            lat_center = format_string.format(center[0])
            lon_center = format_string.format(center[1])
            center_str = lat_center + col_separator + lon_center
            output_row.append(center_str)
        output_row.append('*')
        h.loc[len(h)] = output_row
    return h

format_string = ""

def print_parameters(data_separator):
    col_sep = ["Column separator", "String", "::", "Used to split the columns"]
    acc = ["Accuracy", "int", "5", "Used in the format string"]
    order = ["Order", "String", "TD", "Top down (TD) or bottom up (BU)"]
    pref_cluster_amount = ["Preferred cluster amounts", "array", "[5, 10, 25, 50, 100]", "Cluster levels"]
    b_k_means = ["best k means try", "int", "5", "Amount of iterations"]
    amount_of_cores = ["Amount of cores", "int", "1", "Amount of cores the script can use"]
    description = "Script to generate hierarchies of lat-lon coordinates. The script uses a accuracy parameter that determines the number of digits after the decimal point. The order parameter determines if the script should use top down or bottom up clustering. The preferred cluster amounts parameter is an array that determines the amount of clusters for each level. The best k means try parameter determines the amount of iterations the script should use. The amount of cores parameter determines the amount of cores the script can use. The script uses an MinibathcKMeans algorithm to cluster the data."
    print(data_separator.join(col_sep))
    print(data_separator.join(acc))
    print(data_separator.join(order))
    print(data_separator.join(pref_cluster_amount))
    print(data_separator.join(b_k_means))
    print(data_separator.join(amount_of_cores))
    print(description)
    print('DONE')


def create_hierarchy(parameters) -> pd.DataFrame:
    global format_string

    col_separator = parameters['Column separator']
    accuracy = parameters['Accuracy']
    format_string = "{:." + str(accuracy) + "f}"
    order = parameters['Order']
    preferred_cluster_amounts = parameters['Preferred cluster amounts']
    best_k_means_try = parameters['best k means try']
    os.environ["LOKY_MAX_CPU_COUNT"] = parameters['Amount of cores']

    # read the data
    data = []
    data_size = int(input())
    for i in range(data_size):
        data.append(input())

    
    lat_list = []
    lon_list = []
    
    for item in data:
        latitude, longitude = item.split(col_separator)
        lat_list.append(float(latitude))
        lon_list.append(float(longitude))
    X = pd.DataFrame({'latitude': lat_list, 'longitude': lon_list})
    top_down = (order == "TD")
    if top_down:

        label_dict, centers_dict, label_counts_dict = top_down_clustering(X, best_k_means_try, preferred_cluster_amounts)

        hierarchy = get_hierarchy_top_down(X, label_dict, centers_dict, label_counts_dict, col_separator)

    else:

        label_dict, centers_dict, label_counts_dict = bottom_up_clustering(X, best_k_means_try, preferred_cluster_amounts)

        hierarchy = get_hierarchy_bottom_up(X, label_dict, centers_dict, label_counts_dict, col_separator)

    return hierarchy


def print_hierarchy(hierarchy:pd.DataFrame, data_separator):
    for index, row in hierarchy.iterrows():
        print(data_separator.join(row.tolist()))
    print('DONE')


def parse_parameters(data_separator):
    expected_params = ['Column separator','Accuracy','Order','Preferred cluster amounts','best k means try','Amount of cores']

    number_of_parameters = int(input())
    raw_params = []
    for i in range(number_of_parameters):
        raw_params.append(input())
    parameters = dict()
    for item in raw_params:
        splited = item.split(data_separator)
        parameters[splited[0]] = splited[1]
    
    # check if we got all expected parameters
    for param in expected_params:
        if param not in parameters.keys():
            print(f'Missing parameter {param}',file=sys.stderr)
            sys.exit()

    parameters['Accuracy'] = int(parameters['Accuracy'])
    parameters['best k means try'] = int( parameters['best k means try'])
    parameters['Preferred cluster amounts'] = ast.literal_eval(parameters['Preferred cluster amounts'])


    if parameters['Order'] not in ['TD','BU']:
        print('Invalid Order: must be TD or BU',file=sys.stderr)
        sys.exit()

    return parameters



if __name__ == '__main__':
    args = sys.argv

    try:
        data_separator = args[1]
        if '-parameters' in args:
            print_parameters(data_separator)
        else:
            parameters = parse_parameters(data_separator)
            hierarchy = create_hierarchy(parameters)
            print_hierarchy(hierarchy, data_separator)
    except Exception as e:
        print(e, file=sys.stderr)
        print(traceback.format_exc(),file=sys.stderr)
