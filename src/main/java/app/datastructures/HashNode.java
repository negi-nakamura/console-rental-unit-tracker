package app.datastructures;

class HashNode<K, V> {
    final K key;
    V value;
    HashNode<K, V> next;

    /** Creates a hash node entry.*/
    HashNode(K key, V value, HashNode<K, V> next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }
}