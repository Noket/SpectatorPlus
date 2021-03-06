package com.pgcraft.spectatorplus;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class SpectatorPlus extends JavaPlugin {
	public HashMap <String, PlayerObject> user = new HashMap<String, PlayerObject>();
	String basePrefix = ChatColor.BLUE + "Spectator" + ChatColor.DARK_BLUE + "Plus";
	String prefix = ChatColor.GOLD + "[" + basePrefix + ChatColor.GOLD + "] ";
	ConsoleCommandSender console;
	ConfigAccessor setup,toggles;	

	// manage toggles
	boolean compass;
	boolean clock;
	boolean specchat;
	boolean scoreboard;
	boolean output;
	boolean death;

	ScoreboardManager manager;
	Scoreboard board;
	Team team;

	@Override
	public void onEnable() {
		setup = new ConfigAccessor(this, "setup");
		toggles = new ConfigAccessor(this, "toggles");

		setup.saveDefaultConfig();
		toggles.saveDefaultConfig();

		compass = toggles.getConfig().getBoolean("compass", true);
		clock = toggles.getConfig().getBoolean("arenaclock", true);
		specchat = toggles.getConfig().getBoolean("specchat", true);
		scoreboard = toggles.getConfig().getBoolean("colouredtablist", true);
		output = toggles.getConfig().getBoolean("outputmessages", true);
		death = toggles.getConfig().getBoolean("deathspec", false);

		console = getServer().getConsoleSender();
		if (scoreboard) {
			manager = Bukkit.getScoreboardManager();
			board = manager.getNewScoreboard();
			team = board.registerNewTeam("spec");
			team.setPrefix(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Spec" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY);
		}

		for (Player player : getServer().getOnlinePlayers()) {
			user.put(player.getName(), new PlayerObject());
		}
		getServer().getPluginManager().registerEvents(new SpectateListener(this), this);
		if(output) {console.sendMessage(prefix + "Version " + ChatColor.RED + "1.8" + ChatColor.GOLD + " is enabled!");}
		this.getCommand("spectate").setExecutor(commands);
		this.getCommand("spec").setExecutor(commands);
	}
	@Override
	public void onDisable() {
		if(output) {console.sendMessage(prefix + "Disabling...");}
		for (Player player : getServer().getOnlinePlayers()) {
			for (Player target : getServer().getOnlinePlayers()) {
				target.showPlayer(player);
			}
			if (user.get(player.getName()).spectating) {
				player.removePotionEffect(PotionEffectType.HEAL);
				player.setAllowFlight(false);
				player.setGameMode(getServer().getDefaultGameMode());
				player.getInventory().clear();
				if (scoreboard) {
					team.removePlayer(player);
				}
				loadPlayerInv(player);
				spawnPlayer(player);
				user.get(player.getName()).spectating = false;
			}
		}
	}
	@Override
	public void onLoad() {

	}

	// --------------
	// CUSTOM METHODS
	// --------------

	// custom method to spawn the player in the lobby
	void spawnPlayer(Player player) {
		player.setFireTicks(0);
		if (setup.getConfig().getBoolean("active") == true) {
			Location where = new Location(getServer().getWorld(setup.getConfig().getString("world")), setup.getConfig().getDouble("xPos"), setup.getConfig().getDouble("yPos"), setup.getConfig().getDouble("zPos"));
			Location aboveWhere = new Location(getServer().getWorld(setup.getConfig().getString("world")), setup.getConfig().getDouble("xPos"), setup.getConfig().getDouble("yPos") + 1, setup.getConfig().getDouble("zPos"));
			Location belowWhere = new Location(getServer().getWorld(setup.getConfig().getString("world")), setup.getConfig().getDouble("xPos"), setup.getConfig().getDouble("yPos") - 1, setup.getConfig().getDouble("zPos"));
			if (where.getBlock().getType() != Material.AIR || aboveWhere.getBlock().getType() != Material.AIR || (belowWhere.getBlock().getType() == Material.AIR || belowWhere.getBlock().getType() == Material.LAVA || belowWhere.getBlock().getType() == Material.WATER)) {
				while (where.getBlock().getType() != Material.AIR || aboveWhere.getBlock().getType() != Material.AIR || (belowWhere.getBlock().getType() == Material.AIR || belowWhere.getBlock().getType() == Material.LAVA || belowWhere.getBlock().getType() == Material.WATER)) {
					where.setY(where.getY()+1);
					aboveWhere.setY(aboveWhere.getY()+1);
					belowWhere.setY(belowWhere.getY()+1);
					if (where.getY() > getServer().getWorld(setup.getConfig().getString("world")).getHighestBlockYAt(where)) {
						where.setY(where.getY()-2);
						aboveWhere.setY(aboveWhere.getY()-2);
						belowWhere.setY(belowWhere.getY()-2);
					}
				}
			}
			user.get(player.getName()).teleporting = true;
			player.teleport(where);

			user.get(player.getName()).teleporting = false;
		} else {
			player.performCommand("spawn");
		}
	}

	// player head inventory method 
	void tpPlayer(Player spectator, int region) {
		Inventory gui = null;
		for (Player player : getServer().getOnlinePlayers()) {
			if (setup.getConfig().getString("mode").equals("any")) {
				if (gui == null) gui = Bukkit.getServer().createInventory(spectator, 27, ChatColor.BLACK + "Teleporter");
				if (player.hasPermission("spectate.hide") == false && user.get(player.getName()).spectating == false) {
					ItemStack playerhead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
					SkullMeta meta = (SkullMeta)playerhead.getItemMeta();
					meta.setOwner(player.getName());
					meta.setDisplayName(player.getName());
					playerhead.setItemMeta(meta);
					gui.addItem(playerhead);
				}
			} else if (setup.getConfig().getString("mode").equals("arena")) {
				if (region == 0) {
					if(output) {spectator.sendMessage(prefix + "Pick an arena first using the clock!");}
					return;
				} else {
					if (gui == null) gui = Bukkit.getServer().createInventory(spectator, 27, ChatColor.BLACK + "Arena " + ChatColor.ITALIC + setup.getConfig().getString("arena." + region + ".name"));
					Location where = player.getLocation();
					int pos1y = setup.getConfig().getInt("arena." + region + ".1.y");
					int pos2y = setup.getConfig().getInt("arena." + region + ".2.y");
					int pos1x = setup.getConfig().getInt("arena." + region + ".1.x");
					int pos2x = setup.getConfig().getInt("arena." + region + ".2.x");
					int pos1z = setup.getConfig().getInt("arena." + region + ".1.z");
					int pos2z = setup.getConfig().getInt("arena." + region + ".2.z");
					// pos1 should have the highest co-ords of the arena, pos2 the lowest
					if (player.hasPermission("spectate.hide") == false && user.get(player.getName()).spectating == false) {
						if (Math.floor(where.getY()) < Math.floor(pos1y) && Math.floor(where.getY()) > Math.floor(pos2y)) {
							if (Math.floor(where.getX()) < pos1x && Math.floor(where.getX()) > pos2x) {
								if (Math.floor(where.getZ()) < pos1z && Math.floor(where.getZ()) > pos2z) {
									ItemStack playerhead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
									SkullMeta meta = (SkullMeta)playerhead.getItemMeta();
									meta.setOwner(player.getName());
									meta.setDisplayName(player.getName());
									playerhead.setItemMeta(meta);
									gui.addItem(playerhead);
								}
							}
						}
					}
				}
			}
		}
		spectator.openInventory(gui);
	}

	void arenaSelect(Player spectator) {
		Inventory gui = Bukkit.getServer().createInventory(spectator, 27, basePrefix);
		for (int i=1; i<setup.getConfig().getInt("nextarena"); i++) {
			ItemStack arenaBook = new ItemStack(Material.BOOK, 1);
			ItemMeta meta = (ItemMeta)arenaBook.getItemMeta();
			meta.setDisplayName(setup.getConfig().getString("arena." + i + ".name"));
			arenaBook.setItemMeta(meta);
			gui.addItem(arenaBook);
		}
		spectator.openInventory(gui);
	}


	// command
	CommandExecutor commands = new CommandExecutor() {
		public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
			// On command: to the sender of the command
			if (sender instanceof Player && sender.hasPermission("spectate.use")) {
				Player spectator = getServer().getPlayer(sender.getName());
				if (args.length > 0 && args[0].equals("on")) {
					if (args.length == 1) {
						enableSpectate((Player) sender, sender);
					} else if (getServer().getPlayer(args[1]) != null) {
						enableSpectate(getServer().getPlayer(args[1]), sender);
					} else {
						sender.sendMessage(prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online");
					}
				} else if (args.length > 0 && args[0].equals("off")) {
					if (args.length == 1) {
						disableSpectate((Player) sender, sender);
					} else if (getServer().getPlayer(args[1]) != null) {
						disableSpectate(getServer().getPlayer(args[1]), sender);
					} else {
						sender.sendMessage(prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online");
					}
				} else if (args.length == 1 && args[0].equals("lobby")) {
					if (sender.hasPermission("spectate.set")) {
						spectator.sendMessage(prefix + "Usage: " + ChatColor.RED + "/spectate lobby <set/del>");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length == 2 && args[0].equals("lobby") && args[1].equals("set")) {
					if (sender.hasPermission("spectate.set")) {
						Location where = spectator.getLocation();
						setup.getConfig().set("xPos", Math.floor(where.getX())+0.5);
						setup.getConfig().set("yPos", Math.floor(where.getY()));
						setup.getConfig().set("zPos", Math.floor(where.getZ())+0.5);
						setup.getConfig().set("world", where.getWorld().getName());
						setup.getConfig().set("active", true);
						setup.saveConfig();
						spectator.sendMessage(prefix + "Location saved! Players will be teleported here when they spectate");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to set the lobby location!");
					}
				} else if (args.length == 2 && args[0].equals("lobby") && (args[1].equals("del") || args[1].equals("delete"))) {
					if (sender.hasPermission("spectate.set")) {
						setup.getConfig().set("xPos", 0);
						setup.getConfig().set("yPos", 0);
						setup.getConfig().set("zPos", 0);
						setup.getConfig().set("world", null);
						setup.getConfig().set("active", false);
						setup.saveConfig();
						spectator.sendMessage(prefix + "Spectator lobby location removed! Players will be teleported to spawn when they spectate");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to set the lobby location!");
					}
				} else if (args.length == 1 && args[0].equals("mode")) {
					if (sender.hasPermission("spectate.set")) {
						spectator.sendMessage(prefix + "Usage: " + ChatColor.RED + "/spectate mode <arena/any>");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length > 1 && args[0].equals("mode") && args[1].equals("any")) {
					if (sender.hasPermission("spectate.set")) {
						setup.getConfig().set("mode", "any");
						setup.saveConfig();
						spectator.sendMessage(prefix + "Mode set to " + ChatColor.RED + "any");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length > 1 && args[0].equals("mode") && args[1].equals("arena")) {
					if (sender.hasPermission("spectate.set")) {
						setup.getConfig().set("mode", "arena");
						setup.saveConfig();
						spectator.sendMessage(prefix + "Mode set to " + ChatColor.RED + "arena" + ChatColor.GOLD + ". Only players in arena regions can be teleported to by spectators.");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if ((args.length == 1 && args[0].equals("arena")) || (args.length == 2 && args[0].equals("arena") && args[1].equals("add"))) {
					if (sender.hasPermission("spectate.set")) {
						spectator.sendMessage(prefix + "Usage: " + ChatColor.RED + "/spectate arena <add <name>/reset/lobby <id>/list>");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length == 3 && args[0].equals("arena") && args[1].equals("add")) {
					if (sender.hasPermission("spectate.set")) {
						user.get(spectator.getName()).arenaName = args[2];
						spectator.sendMessage(prefix + "Punch point " + ChatColor.RED + "#1" + ChatColor.GOLD + " - a corner of the arena");
						user.get(spectator.getName()).setup = 1;
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length == 2 && args[0].equals("arena") && args[1].equals("list")) {
					if (sender.hasPermission("spectate.set")) {
						spectator.sendMessage(ChatColor.GOLD + "          ~~ " + ChatColor.RED + "Arenas" + ChatColor.GOLD + " ~~          ");
						for (int i=1; i<setup.getConfig().getInt("nextarena"); i++) {
							spectator.sendMessage(ChatColor.RED + "(#" + i + ") " + setup.getConfig().getString("arena." + i + ".name") + ChatColor.GOLD + " Lobby x:" + setup.getConfig().getDouble("arena." + i + ".lobby.x") + " y:" + setup.getConfig().getDouble("arena." + i + ".lobby.y") + " z:" + setup.getConfig().getDouble("arena." + i + ".lobby.z"));
						}
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length == 3 && args[0].equals("arena") && args[1].equals("lobby")) {
					if (sender.hasPermission("spectate.set")) {
						lobbySetup(spectator, args[2]);
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else if (args.length == 2 && args[0].equals("arena") && args[1].equals("reset")) {
					if (sender.hasPermission("spectate.set")) {
						setup.getConfig().set("arena", null);
						setup.getConfig().set("nextarena", 1);
						setup.saveConfig();
						spectator.sendMessage(prefix + "All arenas removed.");
					} else {
						spectator.sendMessage(prefix + "You do not have permission to change the mode!");
					}
				} else {
					printHelp(sender);
				}
			} else {
				if (sender instanceof Player) {
					sender.sendMessage(prefix + "You don't have permission to spectate!");
				} else {
					// Console commands
					if (args.length > 0 && args[0].equals("on")) {
						if (args.length == 1) {
							sender.sendMessage(prefix + "Usage: /spec on <player>");
						} else if (getServer().getPlayer(args[1]) != null) {
							enableSpectate(getServer().getPlayer(args[1]), sender);
						} else {
							sender.sendMessage(prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online");
						}
					} else if (args.length > 0 && args[0].equals("off")) {
						if (args.length == 1) {
							sender.sendMessage(prefix + "Usage: /spec off <player>");
						} else if (getServer().getPlayer(args[1]) != null) {
							disableSpectate(getServer().getPlayer(args[1]), sender);
						} else {
							sender.sendMessage(prefix + ChatColor.RED + args[1] + ChatColor.GOLD + " isn't online");
						}
					} else if (args.length == 1 && args[0].equals("mode")) {
						sender.sendMessage(prefix + "Usage: " + ChatColor.RED + "/spectate mode <arena/any>");
					} else if (args.length > 1 && args[0].equals("mode") && args[1].equals("any")) {
						setup.getConfig().set("mode", "any");
						setup.saveConfig();
						sender.sendMessage(prefix + "Mode set to " + ChatColor.RED + "any");
					} else if (args.length > 1 && args[0].equals("mode") && args[1].equals("arena")) {
						setup.getConfig().set("mode", "arena");
						setup.saveConfig();
						sender.sendMessage(prefix + "Mode set to " + ChatColor.RED + "arena" + ChatColor.GOLD + ". Only players in arena regions can be teleported to by spectators.");
					} else {
						printHelp(sender);
					}
				}
			}
			return true; // return true: to stop usage showing
		} // end of onCommand
	};
	void printHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "            ~~ " + ChatColor.BLUE + "Spectator" + ChatColor.DARK_BLUE + "Plus" + ChatColor.GOLD + " ~~            ");
		sender.sendMessage(ChatColor.RED + "/spectate <on/off> [player]" + ChatColor.GOLD + ": Enables/disables spectator mode [for a certain player]");
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.RED + "/spectate arena <add <name>/reset/lobby <id>/list>" + ChatColor.GOLD + ": Adds/deletes arenas");
			sender.sendMessage(ChatColor.RED + "/spectate lobby <set/del>" + ChatColor.GOLD + ": Adds/deletes the spectator lobby");
		}
		sender.sendMessage(ChatColor.RED + "/spectate mode <any/arena>" + ChatColor.GOLD + ": Sets who players can teleport to");
	}
	void disableSpectate(Player spectator, CommandSender sender) {
		if (user.get(spectator.getName()).spectating) {
			// show them to everyone
			for (Player target : getServer().getOnlinePlayers()) { 
				target.showPlayer(spectator);
			}

			// teleport to spawn
			spawnPlayer(spectator);

			// allow interaction
			user.get(spectator.getName()).spectating = false;
			spectator.setGameMode(getServer().getDefaultGameMode());
			spectator.setAllowFlight(false);
			spectator.removePotionEffect(PotionEffectType.HEAL);
			loadPlayerInv(spectator);

			//remove from spec team
			if (scoreboard) {
				team.removePlayer(spectator);
			}

			if (sender instanceof Player && spectator.getName().equals(sender.getName())) {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "disabled");}
			} else if (sender instanceof Player && !spectator.getName().equals(sender.getName())) {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "disabled" + ChatColor.GOLD + " by " + ChatColor.RED + ((Player) sender).getDisplayName());}
				sender.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "disabled" + ChatColor.GOLD + " for " + ChatColor.RED + spectator.getDisplayName());
			} else {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "disabled" + ChatColor.GOLD + " by " + ChatColor.DARK_RED + "Console");}
				sender.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "disabled" + ChatColor.GOLD + " for " + ChatColor.RED + spectator.getDisplayName());
			}

		} else {
			// Spectate mode wasn't on
			if (sender instanceof Player && spectator.getName().equals(sender.getName())) {
				spectator.sendMessage(prefix + "You aren't spectating!");
			} else {
				sender.sendMessage(prefix + ChatColor.RED + spectator.getDisplayName() + ChatColor.GOLD + " isn't spectating!");
			}
		}
	}
	void enableSpectate(Player spectator, CommandSender sender) {
		if (user.get(spectator.getName()).spectating) {
			// Spectate mode wasn't on
			if (sender instanceof Player && spectator.getName().equals(sender.getName())) {
				spectator.sendMessage(prefix + "You are already spectating!");
			} else {
				sender.sendMessage(prefix + ChatColor.RED + spectator.getDisplayName() + ChatColor.GOLD + " is already spectating!");
			}
		} else {
			// teleport them to the global lobby
			spawnPlayer(spectator);
			// hide them from everyone
			for (Player target : getServer().getOnlinePlayers()) {
				target.hidePlayer(spectator);
			}
			// gamemode and inventory
			spectator.setGameMode(GameMode.ADVENTURE);
			savePlayerInv(spectator);
			spectator.setAllowFlight(true);
			spectator.setFoodLevel(20);
			// disable interaction
			user.get(spectator.getName()).spectating = true;
			PotionEffect heal = new PotionEffect(PotionEffectType.HEAL, Integer.MAX_VALUE, 1000, true);
			spectator.addPotionEffect(heal);
			// give them compass if toggle on
			if (compass) {
				ItemStack compass = new ItemStack(Material.COMPASS, 1);
				ItemMeta compassMeta = (ItemMeta)compass.getItemMeta();
				compassMeta.setDisplayName(ChatColor.BLUE + "Teleporter");
				compass.setItemMeta(compassMeta);
				spectator.getInventory().addItem(compass);
			}
			// give them clock (only for arena mode and if toggle is on)
			if (clock) {
				String mode = setup.getConfig().getString("mode");
				if (mode.equals("arena")) {
					ItemStack book = new ItemStack(Material.WATCH, 1);
					ItemMeta bookMeta = (ItemMeta)book.getItemMeta();
					bookMeta.setDisplayName(ChatColor.DARK_RED + "Arena chooser");
					book.setItemMeta(bookMeta);
					spectator.getInventory().addItem(book);
				}
			}
			// set the prefix in the tab list if the toggle is on
			if (scoreboard) {
				team.addPlayer(spectator);
			}
			// manage messages if spectator was enabled
			if (sender instanceof Player && spectator.getName().equals(sender.getName())) {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "enabled");}
			} else if (sender instanceof Player && !spectator.getName().equals(sender.getName())) {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "enabled" + ChatColor.GOLD + " by " + ChatColor.RED + ((Player) sender).getDisplayName());}
				sender.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "enabled" + ChatColor.GOLD + " for " + ChatColor.RED + spectator.getDisplayName());
			} else {
				if(output) {spectator.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "enabled" + ChatColor.GOLD + " by " + ChatColor.DARK_RED + "Console");}
				sender.sendMessage(prefix + "Spectator mode " + ChatColor.RED + "enabled" + ChatColor.GOLD + " for " + ChatColor.RED + spectator.getDisplayName());
			}
		}
	}
	boolean modeSetup(Player player, Block block) {
		if (user.get(player.getName()).setup == 2) {
			user.get(player.getName()).pos2 = block.getLocation();
			user.get(player.getName()).setup = 0;

			Location lowPos, hiPos;
			lowPos = new Location(user.get(player.getName()).pos1.getWorld(), 0, 0, 0);
			hiPos = new Location(user.get(player.getName()).pos1.getWorld(), 0, 0, 0);
			// yPos
			if (Math.floor(user.get(player.getName()).pos1.getY()) > Math.floor(user.get(player.getName()).pos2.getY())) {
				hiPos.setY(Math.floor(user.get(player.getName()).pos1.getY()));
				lowPos.setY(Math.floor(user.get(player.getName()).pos2.getY()));
			} else {
				lowPos.setY(Math.floor(user.get(player.getName()).pos1.getY()));
				hiPos.setY(Math.floor(user.get(player.getName()).pos2.getY()));
			}
			// xPos
			if (Math.floor(user.get(player.getName()).pos1.getX()) > Math.floor(user.get(player.getName()).pos2.getX())) {
				hiPos.setX(Math.floor(user.get(player.getName()).pos1.getX()));
				lowPos.setX(Math.floor(user.get(player.getName()).pos2.getX()));
			} else {
				lowPos.setX(Math.floor(user.get(player.getName()).pos1.getX()));
				hiPos.setX(Math.floor(user.get(player.getName()).pos2.getX()));
			}
			// zPos
			if (Math.floor(user.get(player.getName()).pos1.getZ()) > Math.floor(user.get(player.getName()).pos2.getZ())) {
				hiPos.setZ(Math.floor(user.get(player.getName()).pos1.getZ()));
				lowPos.setZ(Math.floor(user.get(player.getName()).pos2.getZ()));
			} else {
				lowPos.setZ(Math.floor(user.get(player.getName()).pos1.getZ()));
				hiPos.setZ(Math.floor(user.get(player.getName()).pos2.getZ()));
			}

			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".1.y", Math.floor(hiPos.getY()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".1.x", Math.floor(hiPos.getX()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".1.z", Math.floor(hiPos.getZ()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".2.y", Math.floor(lowPos.getY()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".2.x", Math.floor(lowPos.getX()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".2.z", Math.floor(lowPos.getZ()));
			setup.getConfig().set("arena." + setup.getConfig().getInt("nextarena") + ".name", user.get(player.getName()).arenaName);
			setup.getConfig().set("nextarena", setup.getConfig().getInt("nextarena") + 1);
			setup.saveConfig();
			player.sendMessage(prefix + "Arena " + ChatColor.RED + user.get(player.getName()).arenaName + " (#" + (setup.getConfig().getInt("nextarena")-1) + ")" + ChatColor.GOLD + " successfully set up!");
			return true;
		} else {
			if (user.get(player.getName()).setup == 1) {
				user.get(player.getName()).pos1 = block.getLocation();
				player.sendMessage(prefix + "Punch point " + ChatColor.RED + "#2" + ChatColor.GOLD + " - the opposite corner of the arena");
				user.get(player.getName()).setup = 2;
				return true;
			} else {
				return false;
			}
		}
	}
	void lobbySetup(Player player, String arenaName) {
		int arenaNum = 0;
		for (int i=1; i<setup.getConfig().getInt("nextarena"); i++) {
			if (setup.getConfig().getString("arena." + i + ".name").equals(arenaName)) {
				arenaNum = i;
			}
		}
		if (arenaNum == 0) {
			player.sendMessage(prefix + "Arena " + ChatColor.RED + arenaName + ChatColor.GOLD + " doesn't exist!");
		} else {
			setup.getConfig().set("arena." + arenaNum + ".lobby.y", Math.floor(player.getLocation().getY()));
			setup.getConfig().set("arena." + arenaNum + ".lobby.x", Math.floor(player.getLocation().getX()));
			setup.getConfig().set("arena." + arenaNum + ".lobby.z", Math.floor(player.getLocation().getZ()));
			setup.getConfig().set("arena." + arenaNum + ".lobby.world", player.getWorld().getName());
			setup.saveConfig();
			player.sendMessage(prefix + "Arena " + ChatColor.RED + arenaName + ChatColor.GOLD + "'s lobby location set to your location");
		}
	}
	void savePlayerInv(Player player) {
		ItemStack[] inv = player.getInventory().getContents();
		ItemStack[] arm = player.getInventory().getArmorContents();
		user.get(player.getName()).inventory = inv;
		user.get(player.getName()).armour = arm;
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
	}
	@SuppressWarnings("deprecation")
	void loadPlayerInv(Player player) {
		player.getInventory().clear();
		player.getInventory().setContents(user.get(player.getName()).inventory);
		player.getInventory().setArmorContents(user.get(player.getName()).armour);
		user.get(player.getName()).inventory = null;
		user.get(player.getName()).armour = null;
		player.updateInventory(); // yes, it's deprecated. But it still works!
	}
}
