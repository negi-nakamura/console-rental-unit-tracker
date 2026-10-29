package app.datastructures;

import java.util.Objects;
import java.util.function.BiConsumer;

public class HashMap<K, V> {

    private HashNode<K, V>[] buckets;
    private int count;

    /** Creates an empty map with 10 buckets. */
    public HashMap() {
        this(10);
    }

    /** Creates an empty map with the specified number of buckets. */
    @SuppressWarnings("unchecked")
    public HashMap(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }

        buckets = (HashNode<K, V>[]) new HashNode<?, ?>[capacity];
        count = 0;
    }

    /** Converts a non-null key into a valid bucket index. */
    private int hash(K key) {
        Objects.requireNonNull(key, "Key cannot be null.");
        return Math.floorMod(key.hashCode(), buckets.length);
    }

    /** Inserts an entry; rejects duplicates. Time: O(n), Ω(1), Θ(1). Space: Θ(1). */
    public boolean insert(K key, V value) {
        Objects.requireNonNull(value, "Value cannot be null.");

        int index = hash(key);
        HashNode<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return false;
            }

            current = current.next;
        }

        buckets[index] = new HashNode<>(key, value, buckets[index]);
        count++;
        return true;
    }

    /** Returns a value, or null if missing. Time: O(n), Ω(1), Θ(1). Space: Θ(1). */
    public V get(K key) {
        int index = hash(key);
        HashNode<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }

            current = current.next;
        }

        return null;
    }

    /** Updates a value; returns false if missing. Time: O(n), Ω(1), Θ(1). Space: Θ(1). */
    public boolean replace(K key, V value) {
        Objects.requireNonNull(value, "Value cannot be null.");

        int index = hash(key);
        HashNode<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value;
                return true;
            }

            current = current.next;
        }

        return false;
    }

    /** Removes an entry; returns false if missing. Time: O(n), Ω(1), Θ(1). Space: Θ(1). */
    public boolean delete(K key) {
        int index = hash(key);
        HashNode<K, V> current = buckets[index];
        HashNode<K, V> previous = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }

                count--;
                return true;
            }

            previous = current;
            current = current.next;
        }

        return false;
    }

    /** Returns the entry count. Time: O(1), Ω(1), Θ(1). Space: Θ(1). */
    public int size() {
        return count;
    }

    /** Returns true if the map contains no entries. */
    public boolean isEmpty() {
        return count == 0;
    }

    /** Checks whether a key exists. Time: O(n), Ω(1), Θ(1). Space: Θ(1). */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /** Returns the number of buckets in the map. */
    public int getCapacity() {
        return buckets.length;
    }

    /** Returns the entry count divided by the bucket count. */
    public double loadFactor() {
        return (double) count / buckets.length;
    }

    /** Doubles buckets and redistributes nodes. Time: O(n), Ω(n), Θ(n). Space: O(n). */
    @SuppressWarnings("unchecked")
    public void rehash() {
        HashNode<K, V>[] oldBuckets = buckets;

        buckets = (HashNode<K, V>[]) new HashNode<?, ?>[
                Math.multiplyExact(oldBuckets.length, 2)];

        for (HashNode<K, V> head : oldBuckets) {
            HashNode<K, V> current = head;

            while (current != null) {
                HashNode<K, V> next = current.next;
                int index = hash(current.key);

                current.next = buckets[index];
                buckets[index] = current;

                current = next;
            }
        }
    }

    /** Visits every entry without changing the map's structure. Time: O(n), Ω(n), Θ(n). Space: Θ(1), excluding callback work. */
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "Action cannot be null.");

        for (HashNode<K, V> head : buckets) {
            HashNode<K, V> current = head;

            while (current != null) {
                action.accept(current.key, current.value);
                current = current.next;
            }
        }
    }

    /** Builds bucket chains as text. Time: O(n), Ω(n), Θ(n). Space: Θ(n); n includes formatted output. */
    public String getTableLayout() {
        StringBuilder layout = new StringBuilder();

        for (int i = 0; i < buckets.length; i++) {
            layout.append("Bucket ").append(i).append(": ");

            HashNode<K, V> current = buckets[i];

            while (current != null) {
                layout.append("[").append(current.key).append(" = ")
                        .append(current.value).append("] -> ");
                current = current.next;
            }

            layout.append("null").append(System.lineSeparator());
        }

        return layout.toString();
    }

    /** Prints the formatted buckets and chains. Time: O(n), Ω(n), Θ(n). Space: Θ(n). */
    public void displayTable() {
        System.out.print(getTableLayout());
    }
}
