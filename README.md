# Fluid Unit

Forge 1.20.1. Puts every mod's molten metals on **one unit** and **one fluid**.

## What it does

1. **Rescales amounts.** Recipes belonging to a mod listed in `unit.sourceUnits` have their molten
   metal amounts moved to `unit.ingot` (144 by default for gregtech).
2. **Folds fluids.** `tconstruct:molten_iron` and `createmetallurgy:molten_iron` are repointed at
   GregTech's, so a machine on either side can take from the same tank.
3. **Ships the tags.** GregTech registers no `forge:molten_*` fluid tags at all, this mod generates tags tinkers can read.
4. **Fixes the tooltips.** Tinkers' tank tooltips hardcode "an ingot is 90mB" in a resource file.
   The same pack overrides it.

All of it runs on the raw recipe json, before any mod deserialises it, so every mod's own serializer
builds the corrected recipe. There is no per-mod code and no compile dependency on anything bridged.
Mods added later are handled by the same pass.

## What it does not do

- It does not touch fluids that are not ingot denominated. Molten clay, glass, slime and the rest
  keep the values their own mod balanced.
- It does not convert amounts at transfer time.
- It does not rewrite GregTech ingot behavior. GregTech generates its recipes at runtime in the thousands and its
  fluids carry material properties its own machines read. Set `unify.canonicalNamespace` if you want to use a different mod than GT

## Checking it worked

`/fluidunit dump` prints the unit, the mods being rescaled, and how many fluids were folded. The
full mapping — including every molten fluid it could **not** find a counterpart for — goes to the
log.

The startup log carries one line per reload:

```
Fluid unit 144mB/ingot: rescaled 4127 amounts, repointed 918 fluids, resolved 622 output tags
```

Watch for this warning. It means a mod is using a shared molten metal on an unknown unit, which is
exactly how a duplication loop gets back in:

```
These mods use a shared molten metal but are in neither unit list, so their amounts were left
alone: [somemod]. Add each as namespace=millibuckets under unit.sourceUnits, ...
```

## Config

`config/fluidunit-common.toml`.

| Key | Default | What |
| --- | --- | --- |
| `unit.ingot` | `144` | mB per ingot after conversion |
| `unit.sourceUnits` | `tconstruct=90`, `createmetallurgy=90`, `tinkersmetallurgy=90`, `tic3nh=90` | mods written in another unit |
| `unit.nativeNamespaces` | `gtceu` | mods already on the target unit |
| `unify.canonicalNamespace` | `gtceu` | whose fluids win |
| `unify.aliasOverrides` | — | `from=to`; an empty right side pins a fluid |
| `unify.minMoltenTemperature` | `500` | a candidate must actually be hot to count as the melt |
| `metals.metals` | ~60 names | the only fluids that are ever touched |
| `metals.excludedRecipes` | — | recipe ids to skip, trailing `*` matches a prefix |

Edit and `/reload`; nothing needs a restart.

### Without GregTech

Set `unify.canonicalNamespace` to whichever mod should win and `unit.ingot` to its unit. With
`tconstruct` and `90` the mod folds Create: Metallurgy onto Tinkers' and changes no amounts.
