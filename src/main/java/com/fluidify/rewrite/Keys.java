package com.fluidify.rewrite;

import java.util.List;
import java.util.Set;

// the handful of json shapes a fluid stack is written in. mantle uses name/tag, create uses
// fluid/fluidTag, and both spell the size amount, which is what tells a fluid node apart from an
// item one: an item ingredient never carries an amount.
public final class Keys {

    private Keys() {}

    public static final String AMOUNT = "amount";

    public static final String FLUID = "fluid";

    public static final List<String> FLUID_ID = List.of("fluid", "name");

    public static final List<String> FLUID_TAG = List.of("tag", "fluidTag", "fluid_tag");

    public static final Set<String> ITEM_MARKERS = Set.of("item", "items", "count");

    public static final Set<String> OUTPUT = Set.of("result", "results", "output", "outputs");

    public static final Set<String> INPUT =
            Set.of("ingredient", "ingredients", "input", "inputs", "fluid", "fluids", "cast", "catalyst");
}
