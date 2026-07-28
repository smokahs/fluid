# Changelog

## 1.0.0

Initial release.

- Rescales molten metal amounts in recipe json before deserialisation, so every mod's melting and casting mB-per-ingot match. (Default 144mB, matching gregtech. 90mB = other mods like tinkers)
- Folds duplicate molten fluids onto one, resolved from the fluid registry.
- Generates `forge:molten_*` tags for the singular fluid that won.
- Overrides Tinkers' ingot and metal tooltip units to match mod's defaults
- `/fluidify dump` prints the the number of fluid merge sucesses as well as a full log of every molten fluid that didn't resolve a match.
