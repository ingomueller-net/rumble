package org.rumbledb.runtime.functions.object;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.items.ObjectItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ObjectRemoveKeysClosure implements FlatMapFunction<Item, Item> {

    private static final long serialVersionUID = 1L;
    private List<String> removalKeys;
    private ExceptionMetadata itemMetadata;

    public ObjectRemoveKeysClosure(List<String> removalKeys, ExceptionMetadata itemMetadata) {
        this.removalKeys = removalKeys;
        this.itemMetadata = itemMetadata;
    }

    public Iterator<Item> call(Item arg0) throws Exception {
        List<Item> results = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<Item> values = new ArrayList<>();

        if (!arg0.isObject())
            return results.iterator();

        for (String key : arg0.getKeys()) {
            if (!this.removalKeys.contains(key)) {
                keys.add(key);
                values.add(arg0.getItemByKey(key));
            }
        }

        results.add(new ObjectItem(keys, values, this.itemMetadata));
        return results.iterator();
    }
};