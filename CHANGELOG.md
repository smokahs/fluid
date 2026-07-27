# Changelog

## 1.0.0

Initial release.

- Rescales molten metal amounts in recipe json before deserialisation, so every mod's melting and
  casting agree on what an ingot is worth. Default 144mB, GregTech's.
- Folds duplicate molten fluids onto one winner, resolved from the fluid registry rather than a
  shipped table, with a temperature check so a cold chemical of the same name is not mistaken for
  the melt.
- Generates `forge:molten_*` tag membership for the winning fluids. GregTech ships none, so without
  this Tinkers' recipes cannot see its fluids at all.
- Overrides Tinkers' ingot and metal tooltip units so tanks stop claiming an ingot is 90mB.
- `/fluidunit dump` prints the resolved mapping and every molten fluid left unmatched.
- Warns once per reload about mods using a shared molten metal on an unknown unit.
