#!/bin/bash
sed -i 's/val roughInterval = priceRange \/ 8.0/val roughInterval = (priceRange \/ 8.0).toDouble()/' app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt
sed -i 's/val firstGridPrice = kotlin.math.ceil(minPrice \/ interval) \* interval/val firstGridPrice = kotlin.math.ceil(minPrice.toDouble() \/ interval) \* interval/' app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt
sed -i 's/val y = getY(currentGridPrice)/val y = getY(currentGridPrice.toDouble())/' app/src/main/java/com/soc/optionchain/screens/ChartScreen.kt

# Also there's a missing @Composable on SettingsPanel if the script messed up, but wait, let's check
