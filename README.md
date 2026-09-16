# Brains Off Mixology Helper

Brains Off Mixology Helper guides a repeatable, low-attention Mastering Mixology inventory from the first lever pull to the final deposit. You choose the potion totals once; the plugin arranges them into contiguous station batches so you can mix in inventory order, stay at one processing station at a time, and move only when that station's batch is finished.

The helper only appears inside the Mastering Mixology laboratory. It provides overlays and reorders existing menu entries, but it never clicks, moves the mouse, or performs an action for you.

![Brains Off Mixology Helper configuration](tutorial-images/01-configuration.png)

## Installation

Once the plugin is available on the Plugin Hub:

1. Open RuneLite's configuration panel.
2. Select **Plugin Hub** at the bottom.
3. Search for **Brains Off Mixology Helper** and select **Install**.
4. Open the plugin settings and keep the default batch or enter your own potion totals.

The overlays stay hidden outside the minigame, so the plugin can remain enabled between sessions.

## Quick start

1. Enter the Mastering Mixology laboratory. For the simplest first run, begin with an empty inventory.
2. Stand at the mixing vessel and follow the numbered levers. Pull `1`, `2`, `3`, then click the vessel marked `4`.
3. Continue until your available potion slots are filled. The potion queue advances immediately when you click **Mix**.
4. Move to the highlighted processing station. Process that numbered inventory batch from top-left to bottom-right, then follow the highlight to the next station.
5. When every potion is processed, use the green-highlighted conveyor. The helper will begin the next refill around any potions left in your inventory.

## Reading the mixing guidance

![Mixing queue, lever markers, and instruction panel](tutorial-images/02-mixing-guidance.png)

The complete recipe is shown at once. You do not need to wait for each individual instruction to appear.

| On-screen cue | Meaning |
| --- | --- |
| Permanent `M`, `A`, and `L` | Identifies the Mox, Aga, and Lye lever even when it is not currently highlighted. |
| Bright `1`, `2`, and `3` | The lever order for the potion you are making now. Required levers are also outlined. |
| Gray numbers below | The lever order for the next potion in the queue. |
| White `4` on the vessel | Click **Mix** after the three lever pulls. |
| Recipe letters in the instruction panel | The same recipe in compact form, such as `M A L`. |

For a MAL, follow `1` on Mox, `2` on Aga, `3` on Lye, then `4` on the mixing vessel. Repeated ingredients share a lever: an ALA shows `1 / 3` on Aga and `2` on Lye.

If you pull a wrong lever, the intended recipe remains visible instead of allowing the mistake to advance the guide. If you deliberately mix a different potion, the resulting potion is detected and included in the live inventory plan.

### Potion queue

The queue is a look-ahead window rather than a list of individual lever clicks:

- `NOW` is the complete potion being mixed.
- `+1`, `+2`, and `+3` are the next three complete potions.
- Up to two previous potions remain above `NOW`, making it easy to recover your place after looking away.
- Every row includes both the potion code and its full three-letter recipe.

Clicking **Mix** advances the visible plan immediately; it does not wait for the mixing animation to finish. The prediction is then checked against the potion that actually appears in the inventory.

## Why the inventory is ordered by station

The minigame takes potions from the inventory from top-left to bottom-right. Brains Off Mixology Helper uses that order deliberately: all potions assigned to station batch `#1` come first, followed by `#2`, then `#3`.

Each potion receives a small inventory marker showing its batch number. Once mixing is complete, the helper outlines the station for the earliest unfinished potion and keeps that station selected until its contiguous group is done.

![Numbered inventory batches and highlighted processing station](tutorial-images/03-processing-guidance.png)

With the default station order, the 28-potion inventory is:

| Batch | Station | Potions |
| --- | --- | ---: |
| `#1` | Alembic / Crystallise | 10 |
| `#2` | Agitator / Homogenise | 9 |
| `#3` | Retort / Concentrate | 9 |

The Retort is last by default. Its repeated-click interaction is therefore at the end of the inventory, where an extra click cannot pull a potion belonging to a later station batch.

## Safe repeated clicking with the station guard

![Check swapped into the left-click position on an unavailable station](tutorial-images/04-menu-guard.png)

The optional **Guard wrong stations** setting is designed for processing a station batch with repeated left-clicks:

- When a station is currently usable, its normal **Crystallise**, **Homogenise**, or **Concentrate** action remains the default left-click.
- When that station should not be used, the plugin moves the station's existing **Check** entry into the default position.
- The processing action is not deleted or duplicated; it remains available in the right-click menu.
- The swap is matched to the exact machine under the cursor, so entries from two nearby scene objects cannot be mixed together.

This reduces spillover mistakes while repeatedly clicking a station. As the guide advances to another station, the old station becomes **Check** and the newly required station regains its normal processing action. A station that already contains a potion remains usable so you can always finish or recover it.

Disable **Guard wrong stations** if you prefer the game's original menu order at all times.

## Partial inventories and rolling refills

You do not have to finish filling an inventory before processing it. The earliest station needed by any unfinished potion remains available. Clicking that station tells the helper to finish the current partial inventory, continue through any later station batches, and then guide you to the conveyor.

Existing potions are carried into the next plan:

- Finished and unfinished potions already in the inventory count toward the configured totals.
- Potions temporarily inside a processing station stay accounted for.
- A potion mixed outside the suggested order is recognized by its actual type.
- Digweed and other non-potion items reduce the available potion capacity without stopping the guide. Moving an item transfers the blocked slot instead of shrinking the plan again.

During delivery, Mixalot potions are ignored for the automatic refill threshold. A new mixing queue begins when only Mixalots plus at most two other processed potions remain. This lets the next inventory start before every useful leftover has disappeared.

## Finished potions and order fulfillment

The panel and conveyor can show `Potions: X`, the current order-fulfillment status, both, or neither. **Potion summary** offers `Hidden`, `Count`, `Fulfillment`, and `Both`, with `Both` as the default. The batch number remains planning guidance: it identifies the station assigned by the inventory plan, but does not prove that the potion was processed there.

The fulfillment status compares finished potions in your inventory with the three current orders:

- **Ready** means at least one finished potion has a confirmed recipe and processing type matching an order.
- **Unknown** means there is no confirmed match, but a finished potion has a requested recipe whose processing type has not been identified. Unknown potions for unrelated recipes do not affect the status.
- **No match** means no finished potion is confirmed to fulfill an order and no unidentified potion has a requested recipe.

The helper can identify processing type by observing a potion complete at a station or by reading the result of a manual **Inspect**. **Left-click Inspect** can promote the game's existing action for `Off`, `Unknown`, or `All` finished potions; it defaults to `Off`. When inspecting several potions with identical descriptions, close the current description before inspecting the next one so RuneLite can observe each result.

**Mark unknown processing type** adds a small dot to unidentified finished potions, and **Show processing details on hover** adds the known type to the item tooltip. These controls are independent and both default to off. **Highlight fulfillable orders** colors individually deliverable order names green and defaults to on.

Processing knowledge is session-local. Logging out, hopping worlds, leaving the laboratory, or restarting the plugin clears it; existing finished potions can be identified again with **Inspect**.

## Configuring a batch

Set a count from `0` to `28` for each potion. The combined total must be no more than 28.

Copies are distributed round-robin over the three stations and then flattened into station order:

- `3` copies gives one potion to each station.
- `6` copies gives two to each station.
- `4` copies gives two to the first station and one to each remaining station.

The defaults reproduce the reference 28-potion inventory:

| Potion | Copies | Potion | Copies |
| --- | ---: | --- | ---: |
| MMA | 3 | ALA | 3 |
| MML | 4 | LLL | 3 |
| AAM | 3 | MLL | 3 |
| ALL | 3 | MAL | 6 |
| MMM | 0 | AAA | 0 |
| **Total** | **28** | | |

You may choose any of the six station orders. **Crystallise > Homogenise > Concentrate** is the recommended default for repeated-click processing.

## Settings reference

| Setting | What it controls |
| --- | --- |
| **Station order** | The order of the three contiguous processing batches. |
| **Batch potions** | The target number of each recipe in one inventory. |
| **Show instruction panel** | Current potion, recipe, station assignment, and live counts for every configured potion. |
| **Show scene guidance** | Lever numbers, permanent letters, station outlines, and the conveyor highlight. |
| **Number inventory batches** | The `#1`, `#2`, and `#3` markers on inventory potions. |
| **Show potion queue** | Previous two, current, and next three complete potions. |
| **Guard wrong stations** | Swaps the existing **Check** entry into left-click position when a station is unavailable. |
| **Left-click Inspect** | Promotes **Inspect** for `Off`, `Unknown`, or `All` finished potions; defaults to `Off`. |
| **Highlight fulfillable orders** | Colors order names green when a confirmed matching finished potion is available; enabled by default. |
| **Scene font** | Choose `Default`, `RuneScape`, `RuneScape Small`, or `RuneScape Bold` for all scene hints, including lever letters and recipe numbers. `Default` retains the previous bold styling using RuneLite's configured font family. |
| **Potion summary** | Shows `Potions: X`, fulfillment status, both, or neither in the panel and at the conveyor; defaults to `Both`. |
| **Mark unknown processing type** | Adds a small dot to finished potions whose processing type is unknown; disabled by default. |
| **Show processing details on hover** | Shows a finished potion's known processing type in its hover tooltip; disabled by default. |
| **Station highlight** | Color used for processing-station outlines and the active inventory slot. |
| **Outline width / feather** | Thickness and softness of scene outlines. |

## Troubleshooting and compatibility

- **Nothing is visible:** the helper activates only when you are inside the laboratory and the Mastering Mixology order interface is open.
- **The panel says Paused:** read the red correction message. Common causes include more potions than the configured batch, a potion moved outside the projected slots, or more than one station holding a potion.
- **I want to abandon a nearly complete delivery:** pull any ingredient lever. The helper starts a rolling refill around the potions still present.
- **I want to process only what I have:** use the one highlighted station that remains available for the partial inventory.
- **Another Mixology plugin overlaps the inventory:** disable that plugin's inventory recipe labels or disable **Number inventory batches** here. The established Mastering Mixology plugin can still provide its order UI, quick-action timing, and Digweed alerts.

The plugin does not send account or gameplay data anywhere. It reads the local inventory and minigame state needed for its overlays and does not access login credentials.

## Development

Requirements: Java 11.

```text
./gradlew clean build
```

To launch a development RuneLite client, run `MixologyBatchPluginTest.main` from VS Code or another Java IDE. Assertions must be enabled with `-ea`.

The project uses the Plugin Hub's `standard` build and has no third-party runtime dependencies. Before submission, set the public author in `runelite-plugin.properties`, publish the repository, and add a Plugin Hub manifest containing its URL and a full commit hash.

## Acknowledgements

The implementation was informed by the open-source Mastering Mixology Helper, Noot's Mixology, and Simplifying Mixology plugins. Their notices are recorded in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
