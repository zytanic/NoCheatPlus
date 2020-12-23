/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.command.admin.debug;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.command.CommandUtil;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.IdUtil;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

public class DebugPlayerCommand extends BaseCommand {

    static class DebugEntry {
        public AlmostBoolean active = AlmostBoolean.YES;
        public final Set<CheckType> checkTypes = new LinkedHashSet<CheckType>();

        /**
         * AlmostBoolean[:CheckType1[:CheckType2[...]]]
         * @param input
         * @return
         */
        public static DebugEntry parseEntry(String input) {
            String[] split = input.split(":");
            DebugEntry entry = new DebugEntry();
            entry.active = AlmostBoolean.match(split[0]);
            if (entry.active == null) {
                return null;
            }
            for (int i = 1; i < split.length; i++) {
                try {
                    CheckType checkType = CheckType.valueOf(split[i].toUpperCase().replace('.', '_'));
                    if (checkType == null) {
                        // TODO: Possible !?
                        return entry;
                    }
                    entry.checkTypes.add(checkType);
                }
                catch (Exception e){
                    return entry;
                }
            }
            return entry;
        }

    }

    public DebugPlayerCommand(JavaPlugin plugin) {
        super(plugin, "player", null);
        usage = "/ncp debug player ... online player name or UUID, (yes|no|default)[:CheckType1[:CheckType2...]] to set the default behavior - mix with player names/ids.";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 4) {
            String[] parts = args[3].split(":");
            if (parts.length == 1) {
                return Arrays.asList("no", "yes", "default");
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    builder.append(parts[i]);
                    builder.append(":");
                }
                return CommandUtil.getCheckTypeTabMatches2(parts[parts.length-1], builder.toString());
            }
        }
        return null; // Tab-complete player names. 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        // TODO: Wild cards (all players)?
        // TODO: (Allow to specify OverrideType ?)

        // Note that MAYBE means to reset here, it's not the same as direct PlayerData API access.
        DebugEntry entry = new DebugEntry();
        Player player = null;
        if (args.length > 2) {
            final String input = args[2];
            if (IdUtil.isValidMinecraftUserName(input)) {
                player = DataManager.getPlayer(input);
            }
            else {
                UUID id = IdUtil.UUIDFromStringSafe(input);
                if (id == null) {
                    sender.sendMessage("Bad name or UUID: " + input);
                    return true;
                }
                else {
                    player = DataManager.getPlayer(id);
                }
            }
            if (player == null) {
                sender.sendMessage("Not online: " + input);
                return true;
            }
        } else if (args.length <= 2) {
            sender.sendMessage("Bad setup!");
            return true;
        }

        if (args.length > 3) {
            String input = args[3];
            entry = DebugEntry.parseEntry(input);
            if (entry == null) {
                sender.sendMessage("Bad setup: " + input);
                // Can't continue.
                return true;
            }
        }

        // Execute for online player.
        final Collection<CheckType> checkTypes;
        if (entry.checkTypes.isEmpty()) {
            // CheckType.ALL
            checkTypes = Arrays.asList(CheckType.ALL);
        }
        else {
            checkTypes = entry.checkTypes;
        }
        final IPlayerData data = DataManager.getPlayerData(player);
        for (final CheckType checkType : checkTypes) {
            if (entry.active == AlmostBoolean.MAYBE) {
                data.resetDebug(checkType);
            }
            else {
                data.overrideDebug(checkType, entry.active, 
                        OverrideType.CUSTOM, true);
            }
        }
        sender.sendMessage("Set debug = " + entry.active + " for player " + player.getName() + " for checks: " + StringUtil.join(checkTypes, ","));
        return true;
    }

}
