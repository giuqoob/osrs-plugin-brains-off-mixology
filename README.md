# Brains Off Mixology Helper

Brains Off Mixology Helper guides a repeatable, low-attention Mastering Mixology inventory from the first lever pull to the final deposit. You choose the potion totals once; the plugin arranges them into contiguous station batches so you can mix in inventory order, stay at one processing station at a time, and move only when that station's batch is finished.

It can also follow the three current conveyor orders: it tells you when your inventory is worth delivering, starts the next refill when it isn't, and brews potions for current orders that your stock can't cover.

The helper only appears inside the Mastering Mixology laboratory. It provides overlays, notifications and reorders existing menu entries, but it never clicks, moves the mouse, or performs an action for you.

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

With **Use current orders** on (the default), the conveyor is only highlighted when enough orders can be filled; otherwise the next refill starts straight away. See [Order awareness](#order-awareness).

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

With **Use current orders** on, this rule is replaced: the refill starts exactly when the remaining potions can't fill enough orders. See [Deliver or refill](#deliver-or-refill).

## Order awareness

The plugin reads the three current orders from the game, each a potion that must be processed at a specific station. A processed potion only fills an order when it was processed at that order's station, so the plugin remembers which station processed each potion in your inventory.

All order features are controlled by **Use current orders**. With it off, the plugin behaves exactly as described in the sections above.

### Orders panel

The instruction panel lists the three orders, for example `MMA  Agitator`, each marked:

| Mark | Meaning |
| --- | --- |
| **Ready** | A matching processed potion is in your inventory. |
| **Missing** | No matching potion yet. |
| **Skipped** | The potion is on your **Skip order potions** list and won't be brewed. |

A **Ready** line above them shows how many orders you can fill and how many are needed to deliver.

### Deliver or refill

Once every potion in the inventory is processed, the plugin compares the fillable orders with **Deliver threshold**:

| Situation | Conveyor | Panel |
| --- | --- | --- |
| Enough orders are fillable | Green, `DELIVER n` | **Deliver** |
| Too few are fillable and there is room to mix | Not highlighted; the next refill starts | **Refill** with `ready/needed` |
| Every order is a skipped potion, or nothing matches and there is no room to mix | Orange, `WASTES <potion>` | **No order ready**, naming the potion that will be used |

A deposit with no matching order uses the first potion in your inventory for a reduced reward, which is why that case is shown in orange and names the potion.

The threshold is a target. Orders for skipped potions that you can't fill don't count towards it, so with orders **MMM, MMM, MML** and a threshold of 2, a single ready MML is enough to deliver.

### Brewing current orders

With **Brew current orders** on, each refill checks the current orders. Any order your configured stock can't cover is added to the mixing queue; duplicate orders count separately, and potions on **Skip order potions** (MMM and AAA by default) are never brewed for orders.

Order potions are mixed after all stock, so they sit at the end of the inventory and the stock layout never changes. They are marked `#n ORD` in green and processed at the end of their station's batch:

1. Process the station's stock as usual.
2. The station then shows `USE SLOT n` and the order potion's slot is outlined. Use that potion on the station. While this step is active the station's **Check** entry is in the left-click position, so a repeated left-click can't process the next station's stock by mistake.
3. Continue with the next station.

When the order potion is the only unfinished potion ahead of the station, the step is an ordinary left-click instead.

If an order potion doesn't fit after the stock, the panel shows **No room** instead of dropping any stock.

### Conveyor guard

**Guard conveyor** (off by default) swaps the conveyor's existing **Examine** entry into the left-click position while a refill is due, so a stray click doesn't deposit. **Fulfil-order** stays available in the right-click menu, and the guard is lifted as soon as delivering is worthwhile or a waste deposit is suggested.

### Recommended experience setup

The wiki's experience-per-hour strategy prepares every potion type in advance and fills the remaining space with potions for the current orders. A matching configuration:

| Potion | Copies | Potion | Copies |
| --- | ---: | --- | ---: |
| MMA | 3 | ALA | 3 |
| MML | 3 | MLL | 3 |
| AAM | 3 | ALL | 3 |
| MAL | 4 | LLL, MMM, AAA | 0 |
| **Total** | **22** | | |

This leaves room for up to three order potions plus pastes or Digweed. Set **Deliver threshold** to 2 to favour turning in at least two orders at a time.

## Quick actions and Digweed

**Highlight quick action** outlines the Agitator or Alembic in the **Quick action color** while its timed quick-action click is available. The outline returns to the normal station colour after the click lands, misses, or the window passes. The Retort has no quick action.

**Highlight Digweed** outlines a spawned Digweed in its lab corner, labels it `DIGWEED`, and names the corner in the panel. **Notify Digweed** sends a RuneLite notification when one spawns.

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
| **Station highlight** | Color used for processing-station outlines and the active inventory slot. |
| **Highlight quick action / Quick action color** | Outlines the Agitator or Alembic while its quick-action click is available. |
| **Outline width / feather** | Thickness and softness of scene outlines. |
| **Highlight Digweed / Digweed color** | Outlines a spawned Digweed and names its corner in the panel. |
| **Notify Digweed** | RuneLite notification when a Digweed spawns. |
| **Use current orders** | Reads the three conveyor orders. Turning it off disables every order feature below. |
| **Deliver threshold** | How many orders must be fillable before the conveyor is highlighted (1–3). |
| **Skip order potions** | Potions never brewed for orders. Defaults to MMM and AAA. |
| **Brew current orders** | Adds potions for uncovered orders after the stock in each refill. |
| **Guard conveyor** | Swaps the conveyor's **Examine** entry into left-click while a refill is due. |

## Troubleshooting and compatibility

- **Nothing is visible:** the helper activates only when you are inside the laboratory and the Mastering Mixology order interface is open.
- **The panel says Paused:** read the red correction message. Common causes include more potions than the configured batch, a potion moved outside the projected slots, or more than one station holding a potion.
- **I want to abandon a nearly complete delivery:** pull any ingredient lever. The helper starts a rolling refill around the potions still present.
- **I want to process only what I have:** use the one highlighted station that remains available for the partial inventory.
- **The conveyor isn't highlighted:** with **Use current orders** on, the panel's **Refill** line shows how many orders are ready and how many are needed. Lower **Deliver threshold** or follow the refill.
- **The panel says Paused because of extra potions:** an order potion that wasn't delivered before the orders changed can leave more potions than the batch expects. Deliver or drop it to continue.
- **Another Mixology plugin overlaps the inventory:** disable that plugin's inventory recipe labels or disable **Number inventory batches** here.

The plugin does not send account or gameplay data anywhere. It reads the local inventory and minigame state needed for its overlays and does not access login credentials.

## Development

Requirements: Java 11.

```text
./gradlew clean build
```

To launch a development RuneLite client, run `MixologyBatchPluginTest.main` from VS Code or another Java IDE. Assertions must be enabled with `-ea`. Add `-da:net.runelite.client.plugins.menuentryswapper...` as well: with assertions on, the built-in Menu Entry Swapper can fail an assertion at login and freeze the client.

To log in with a Jagex account, launch RuneLite once from the Jagex Launcher with the client argument `--insecure-write-credentials`; the development client then reads `.runelite/credentials.properties`. Remove the argument and delete that file when you are done, as it holds your session token.

The project uses the Plugin Hub's `standard` build and has no third-party runtime dependencies. Before submission, set the public author in `runelite-plugin.properties`, publish the repository, and add a Plugin Hub manifest containing its URL and a full commit hash.

## Acknowledgements

The implementation was informed by the open-source Mastering Mixology Helper, Noot's Mixology, and Simplifying Mixology plugins. Their notices are recorded in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
