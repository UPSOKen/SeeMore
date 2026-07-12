package com.froobworld.seemore.underground;

import com.froobworld.seemore.config.NaturalCeilingSettings;

import java.util.function.IntFunction;

public final class NaturalCeilingDetector {
    private NaturalCeilingDetector() {
    }

    public static boolean hasNaturalCeiling(IntFunction<CeilingBlock> column,
                                            NaturalCeilingSettings settings,
                                            NaturalCeilingClassifier classifier) {
        for (int offset = 1; offset <= settings.searchDistance(); offset++) {
            CeilingBlock ceiling = column.apply(offset);
            if (ceiling == null) {
                return false;
            }
            if (ceiling.passable()) {
                continue;
            }
            if (!classifier.isNatural(ceiling.material(), settings)) {
                return false;
            }
            for (int thickness = 1; thickness < settings.minimumThickness(); thickness++) {
                CeilingBlock supportingBlock = column.apply(offset + thickness);
                if (supportingBlock == null || supportingBlock.passable()
                        || !classifier.isNatural(supportingBlock.material(), settings)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
