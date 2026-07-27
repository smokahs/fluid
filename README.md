# Fluid Unit

Forge 1.20.1. Puts every mod's molten metals on **one unit** and **one fluid**.

## The problem

| Mod | mB per ingot |
| --- | --- |
| GregTech CEu (`GTValues.L`) | **144** |
| Tinkers' Construct (`FluidValues.INGOT`) | **90** |
| Create: Metallurgy | **90** |

That is not just an annoyance. Once any two of those share a fluid, or a tag, you have a duplication
loop: melt an ingot on the 144 side, cast it on the 90 side, get 1.6 ingots back, repeat. Unifying
the fluids without unifying the unit makes the problem worse, not better.

90 to 144 is a multiply by eight fifths. Every standard Tinkers' amount is a multiple of five and
every GregTech one a multiple of sixteen, so the usual values convert exactly in both directions.

## What it does

1. **Rescales amounts.** Recipes belonging to a mod listed in `unit.sourceUnits` have their molten
   metal amounts moved onto `unit.ingot` (144 by default).
2. **Folds fluids.** `tconstruct:molten_iron` and `createmetallurgy:molten_iron` are repointed at
   GregTech's, so a machine on either side can drink from the same tank.
3. **Ships the tags.** GregTech registers no `forge:molten_*` fluid tags at all, so Tinkers'
   recipes — which ask for the tag, not the fluid — could not see GregTech's fluids no matter what
   the recipes said. A generated built-in pack adds them.
4. **Fixes the tooltips.** Tinkers' tank tooltips hardcode "an ingot is 90mB" in a resource file.
   The same pack overrides it.

All of it runs on the raw recipe json, before any mod deserialises it, so every mod's own serializer
builds the corrected recipe. There is no per-mod code and no compile dependency on anything bridged.
Mods added later are handled by the same pass.

## What it does not do

- It does not touch fluids that are not ingot denominated. Molten clay, glass, slime and the rest
  keep the values their own mod balanced.
- It does not convert amounts at transfer time. A wrapping fluid handler rounds, leaks and is
  invisible to recipe viewers; the conversion belongs in the recipes.
- It does not rewrite GregTech. GregTech generates its recipes at runtime in the thousands and its
  fluids carry material properties its own machines read. It is the wrong side of the lever, so it
  is the side everything else moves onto. Set `unify.canonicalNamespace` if you disagree.

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
