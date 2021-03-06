package exodus;

import objects.BaseMap;
import objects.Creature;
import objects.Person;
import objects.Tile;
import util.Utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import exodus.Party.PartyMember;
import java.util.Map;
import objects.SaveGame;
import util.PartyDeathException;

public class SecondaryInputProcessor extends InputAdapter implements Constants {

    private final BaseScreen screen;
    private final Stage stage;

    private int initialKeyCode;
    private BaseMap bm;
    private int currentX;
    private int currentY;
    private PartyMember member;
    private StringBuilder buffer;

    //only for dungeon screen
    private DungeonTile dngTile;

    public SecondaryInputProcessor(BaseScreen screen, Stage stage) {
        this.screen = screen;
        this.stage = stage;
    }

    public void setinitialKeyCode(int k, BaseMap bm, int x, int y) {

        this.initialKeyCode = k;
        this.bm = bm;
        this.currentX = x;
        this.currentY = y;
        this.member = null;
        this.buffer = new StringBuilder();

        switch (k) {
            case Keys.T:
                screen.log("TALK> Which party member? (1-4)");
                break;
            case Keys.L:
                screen.log("OPEN> ");
                break;
            case Keys.O:
                screen.log("OTHER COMMAND> Which party member? (1-4)");
                break;
            case Keys.U:
                screen.log("UNLOCK> Which party member? (1-4)");
                break;
            case Keys.S:
                screen.log("SEARCH> Which party member? (1-4)");
                break;
            case Keys.A:
                screen.log("ATTACK> ");
                break;
            case Keys.G:
                screen.log("GET> Which party member? (1-4)");
                break;
            case Keys.R:
                screen.log("READY> Which party member? (1-4)");
                break;
            case Keys.W:
                screen.log("WEAR> Which party member? (1-4)");
                break;
            case Keys.Y:
                screen.log("YELL> Which party member? (1-4)");
                break;
            case Keys.C:
                screen.log("CAST> Which party member? (1-4)");
                break;
            case Keys.J:
                screen.log("JOIN GOLD> To party member? (1-4)");
                break;
        }
    }

    public void setinitialKeyCode(int k, DungeonTile dngTile, int x, int y) {
        this.initialKeyCode = k;
        this.dngTile = dngTile;
        this.currentX = x;
        this.currentY = y;
        buffer = new StringBuilder();

        switch (k) {
            case Keys.G:
                screen.log("GET> Which party member? (1-4)");
                break;
            case Keys.R:
                screen.log("READY> Which party member? (1-4)");
                break;
            case Keys.W:
                screen.log("WEAR> Which party member? (1-4)");
                break;
            case Keys.C:
                screen.log("CAST> Which party member? (1-4)");
                break;
            case Keys.S:
                if (dngTile.getValue() >= 144 && dngTile.getValue() <= 148) {
                    screen.log("You find a Fountain. Who drinks? (1-4)");
                } else if (dngTile == DungeonTile.ORB) {
                    screen.log("You find a Magical Orb...Who touches? (1-4)");
                } else {
                    screen.log("Who searches? (1-4)");
                }
                break;
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        Direction dir = Direction.NORTH;

        int x = currentX, y = currentY;

        if (keycode == Keys.UP) {
            dir = Direction.NORTH;
            y = y - 1;
        } else if (keycode == Keys.DOWN) {
            dir = Direction.SOUTH;
            y = y + 1;
        } else if (keycode == Keys.LEFT) {
            dir = Direction.WEST;
            x = x - 1;
        } else if (keycode == Keys.RIGHT) {
            dir = Direction.EAST;
            x = x + 1;
        }

        if (screen.scType == ScreenType.MAIN) {

            GameScreen gameScreen = (GameScreen) screen;

            Window dialog = null;

            if (initialKeyCode == Keys.T) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    member = gameScreen.context.getParty().getMember(keycode - 7 - 1);
                    screen.log("Direction? ");
                    return false;
                }

                if (this.member != null) {
                    screen.logAppend(dir.toString());

                    Tile tile = bm.getTile(x, y);
                    if (tile.getRule() == TileRule.signs) {
                        //talking to vendor so get the vendor on other side of sign
                        switch (dir) {
                            case NORTH:
                                y = y - 1;
                                break;
                            case SOUTH:
                                y = y + 1;
                                break;
                            case EAST:
                                x = x + 1;
                                break;
                            case WEST:
                                x = x - 1;
                                break;
                        }
                    }

                    if (bm.getPeople() != null) {
                        Person p = bm.getPersonAt(x, y);
                        if (p != null) {
                            if (p.getVendor() != null) {
                                Gdx.input.setInputProcessor(stage);
                                dialog = new ConversationDialog(this.member, p, (GameScreen) screen, stage).show(stage);
                            } else if (p.getTile().getName().equals("lord_british")) {

                                if (this.member.getPlayer().meetLordBritish()) {
                                    Sounds.play(Sound.MAGIC);
                                    screen.log("Lord British says:\nThou hast been advanced!");
                                } else {
                                    screen.log("Lord British says:\nWelcome my child..\nExperience more! ");
                                }

                            } else if (p.getConversation() != null) {
                                screen.log(p.getConversation());
                            } else {
                                screen.log("Funny, no response! ");
                            }
                        } else {
                            screen.log("Funny, no response! ");
                        }
                    } else {
                        screen.log("Funny, no response! ");
                    }
                } else {
                    screen.log("Nobody selected!");
                }

            } else if (initialKeyCode == Keys.L) {

                screen.logAppend(dir.toString());
                if (bm.openDoor(x, y)) {
                    screen.log("Opened!");
                } else {
                    screen.log("Can't!");
                }

            } else if (initialKeyCode == Keys.U) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    member = gameScreen.context.getParty().getMember(keycode - 7 - 1);
                    screen.log("Direction? ");
                    return false;
                }

                if (this.member != null) {
                    if (member.getPlayer().keys > 0 && bm.unlockDoor(x, y)) {
                        screen.log("Unlocked!");
                        member.getPlayer().keys--;
                    } else {
                        screen.log("Can't!");
                    }
                } else {
                    screen.log("Nobody selected!");
                }

            } else if (initialKeyCode == Keys.S) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    ItemMapLabels l = bm.searchLocation(screen, gameScreen.context.getParty(), screen.context.getParty().getMember(keycode - 7 - 1), currentX, currentY, 0);
                    if (l != null) {
                        screen.log("You found " + l.getDesc() + ".");
                    } else {
                        screen.log("Nothing here!");
                    }
                    return false;
                }

            } else if (initialKeyCode == Keys.Y) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    screen.log("Yell what?");
                    Gdx.input.setInputProcessor(new YellInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), gameScreen));
                    return false;
                } else {
                    screen.log("Nobody selected!");
                }

            } else if (initialKeyCode == Keys.O) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    screen.log("What other command?");
                    Gdx.input.setInputProcessor(new OtherCommandInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), gameScreen));
                    return false;
                } else {
                    screen.log("Nobody selected!");
                }
            } else if (initialKeyCode == Keys.J) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    gameScreen.context.getParty().poolGold(keycode - 7 - 1);
                } else {
                    screen.log("Nobody selected!");
                }

            } else if (initialKeyCode == Keys.C) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    PartyMember pm = screen.context.getParty().getMember(keycode - 7 - 1);
                    Map<String, Spell> spellSelection = Spell.getCastables(pm, bm.getType());
                    if (spellSelection.size() < 1) {
                        screen.log("No spells to cast!");
                    } else {
                        Gdx.input.setInputProcessor(new SpellInputProcessor(gameScreen, screen.context, stage, spellSelection, pm));
                        return false;
                    }
                } else {
                    screen.log("Nobody selected!");
                }

            } else if (initialKeyCode == Keys.R) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    Gdx.input.setInputProcessor(new ReadyWearInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), true));
                    return false;
                }

            } else if (initialKeyCode == Keys.W) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    Gdx.input.setInputProcessor(new ReadyWearInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), false));
                    return false;
                }

            } else if (initialKeyCode == Keys.G) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    gameScreen.getChest(screen.context.getParty().getMember(keycode - 7 - 1), x, y, false);
                }

            } else if (initialKeyCode == Keys.A) {

                screen.logAppend(dir.toString());

                for (Creature c : bm.getCreatures()) {
                    if (c.currentX == x && c.currentY == y) {
                        Maps cm = screen.context.getCombatMap(c, bm, x, y, currentX, currentY);
                        gameScreen.attackAt(cm, c);
                        return false;
                    }
                }

            }

            if (dialog == null) {
                Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
            } else {
                return false;
            }

        } else if (screen.scType == ScreenType.COMBAT) {

            CombatScreen combatScreen = (CombatScreen) screen;

            if (initialKeyCode == Keys.A) {

                screen.log("Attack > " + dir.toString());

                PartyMember attacker = combatScreen.party.getActivePartyMember();
                WeaponType wt = attacker.getPlayer().weapon;

                if (combatScreen.contextMap == Maps.EXODUS && wt != WeaponType.EXOTIC) {
                    Sounds.play(Sound.NEGATIVE_EFFECT);
                    combatScreen.finishPlayerTurn();
                } else {
                    Sounds.play(Sound.PC_ATTACK);
                    int range = wt.getWeapon().getRange();
                    Utils.animateAttack(stage, combatScreen, attacker, dir, x, y, range);
                }

                Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
                return false;

            } else if (initialKeyCode == Keys.U) {

                if (keycode == Keys.ENTER) {
                    if (buffer.length() < 1) {
                        return false;
                    }
                    String useItem = buffer.toString();
                    screen.log(useItem);
                    if (useItem.startsWith("stone")) {
                        screen.log("There are holes for 4 stones.");
                        screen.log("What colors?");
                        screen.log("1: ");
                        buffer = new StringBuilder();
                        //StoneColorsInputAdapter scia = new StoneColorsInputAdapter(combatScreen);
                        //Gdx.input.setInputProcessor(scia);
                    } else {
                        screen.log("Not a usable item!");
                        Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
                        //combatScreen.finishPlayerTurn();
                    }
                } else if (keycode == Keys.BACKSPACE) {
                    if (buffer.length() > 0) {
                        buffer.deleteCharAt(buffer.length() - 1);
                        screen.logDeleteLastChar();
                    }
                } else if (keycode >= 29 && keycode <= 54) {
                    buffer.append(Keys.toString(keycode).toLowerCase());
                    screen.logAppend(Keys.toString(keycode).toLowerCase());
                }

                return false;

            }

        } else if (screen.scType == ScreenType.DUNGEON) {

            DungeonScreen dngScreen = (DungeonScreen) screen;

            if (initialKeyCode == Keys.S) {

                switch (dngTile) {
                    case FOUNTAIN_PLAIN:
                    case FOUNTAIN_HEAL:
                    case FOUNTAIN_ACID:
                    case FOUNTAIN_CURE:
                    case FOUNTAIN_POISON:
                        if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                            dngScreen.dungeonDrinkFountain(dngTile, keycode - 7 - 1);
                        }
                        break;
                    case MARK_KINGS:
                    case MARK_SNAKE:
                    case MARK_FIRE:
                    case MARK_FORCE:
                        if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                            dngScreen.getMark(dngTile, keycode - 7 - 1);
                        }
                        break;
                    default:
                        break;
                }

            } else if (initialKeyCode == Keys.G) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    dngScreen.getChest(screen.context.getParty().getMember(keycode - 7 - 1), currentX, currentY, false);
                }

            } else if (initialKeyCode == Keys.R) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    Gdx.input.setInputProcessor(new ReadyWearInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), true));
                    return false;
                }

            } else if (initialKeyCode == Keys.W) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    Gdx.input.setInputProcessor(new ReadyWearInputAdapter(screen.context.getParty().getMember(keycode - 7 - 1), false));
                    return false;
                }

            } else if (initialKeyCode == Keys.C) {

                if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_4) {
                    PartyMember pm = screen.context.getParty().getMember(keycode - 7 - 1);
                    Map<String, Spell> spellSelection = Spell.getCastables(pm, MapType.dungeon);
                    if (spellSelection.size() < 1) {
                        screen.log("No spells to cast!");
                    } else {
                        Gdx.input.setInputProcessor(new SpellInputProcessor(screen, screen.context, stage, spellSelection, pm));
                        return false;
                    }
                } else {
                    screen.log("Nobody selected!");
                }
            }

            Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));

        }

        screen.finishTurn(currentX, currentY);
        return false;
    }

    private class ReadyWearInputAdapter extends InputAdapter {

        boolean ready;
        PartyMember pm;

        public ReadyWearInputAdapter(PartyMember pm, boolean ready) {
            this.ready = ready;
            this.pm = pm;

            if (ready) {
                for (WeaponType wt : WeaponType.values()) {
                    char ch = (char) ('a' + wt.ordinal());
                    if (wt == WeaponType.NONE) {
                        screen.log(Character.toUpperCase(ch) + " - " + WeaponType.get(ch - 'a'));
                        continue;
                    }
                    if (pm.getPlayer().weapons[wt.ordinal()] > 0) {
                        screen.log(Character.toUpperCase(ch) + " - " + WeaponType.get(ch - 'a'));
                    } else if (pm.getPlayer().weapon == wt) {
                        screen.log(Character.toUpperCase(ch) + " - " + WeaponType.get(ch - 'a'));
                    }
                }
            } else {
                for (ArmorType at : ArmorType.values()) {
                    char ch = (char) ('a' + at.ordinal());
                    if (at == ArmorType.NONE) {
                        screen.log(Character.toUpperCase(ch) + " - " + ArmorType.get(ch - 'a'));
                        continue;
                    }
                    if (pm.getPlayer().armors[at.ordinal()] > 0) {
                        screen.log(Character.toUpperCase(ch) + " - " + ArmorType.get(ch - 'a'));
                    } else if (pm.getPlayer().armor == at) {
                        screen.log(Character.toUpperCase(ch) + " - " + ArmorType.get(ch - 'a'));
                    }
                }
            }
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode >= Keys.A && keycode <= Keys.P) {
                boolean ret = false;
                if (ready) {
                    ret = pm.readyWeapon(keycode - 29);
                } else {
                    ret = pm.wearArmor(keycode - 29);
                }
                if (!ret) {
                    screen.log("Failed!");
                } else {
                    screen.log("Success!");
                }
            }
            Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
            screen.finishTurn(currentX, currentY);
            return false;
        }
    }

    private class YellInputAdapter extends InputAdapter {

        GameScreen screen;
        StringBuilder buffer = new StringBuilder();
        PartyMember pm;

        public YellInputAdapter(PartyMember pm, GameScreen screen) {
            this.screen = screen;
            this.pm = pm;
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == Keys.ENTER) {
                if (buffer.length() < 1) {
                    return false;
                }
                String text = buffer.toString().toUpperCase();

                switch (text) {
                    case "EVOCARE":
                        Tile north = bm.getTile(currentX, currentY - 1);
                        Tile south = bm.getTile(currentX, currentY + 1);
                        if (north.getName().startsWith("GreatEarthSerpent") || south.getName().startsWith("GreatEarthSerpent")) {
                            if (pm.getPlayer().marks[2] > 0) {
                                Sounds.play(Sound.ROCKS);
                                this.screen.replaceTile("water", 41, 230);
                                this.screen.replaceTile("water", 41, 231);
                            } else {
                                Sounds.play(Sound.NEGATIVE_EFFECT);
                                screen.log("No effect.");
                            }
                        } else {
                            screen.log("No effect.");
                        }
                        break;
                    default:
                        screen.log("What?");
                        break;
                }

                Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
                screen.finishTurn(currentX, currentY);

            } else if (keycode == Keys.BACKSPACE) {
                if (buffer.length() > 0) {
                    buffer.deleteCharAt(buffer.length() - 1);
                    screen.logDeleteLastChar();
                }
            } else if (keycode >= 29 && keycode <= 54) {
                buffer.append(Keys.toString(keycode).toUpperCase());
                screen.logAppend(Keys.toString(keycode).toUpperCase());
            }
            return false;
        }
    }

    private class OtherCommandInputAdapter extends InputAdapter {

        GameScreen screen;
        StringBuilder buffer = new StringBuilder();
        PartyMember pm;

        public OtherCommandInputAdapter(PartyMember pm, GameScreen screen) {
            this.screen = screen;
            this.pm = pm;
        }

        @Override
        public boolean keyUp(int keycode) {

            if (keycode == Keys.ENTER) {

                if (buffer.length() < 1) {
                    return false;
                }

                String text = buffer.toString().toUpperCase();

                switch (text) {
                    case "STEAL":
                        break;
                    case "PRAY":
                        if (currentX >= 44 && currentX <= 52 && currentY >= 44 && currentY <= 52 && bm.getId() == Maps.YEW.getId()) {
                            this.screen.log("Yell 'EVOCARE'");
                        } else {
                            this.screen.log("A calmness and silence of the soul.");
                        }
                        break;
                    case "BRIBE":
                        break;
                    case "INSERT":
                        if (currentX >= 30 && currentX <= 33 && currentY == 12 && bm.getId() == Maps.EXODUS.getId()) {
                            screen.log("D, S, L, M:");
                            Gdx.input.setInputProcessor(new InsertCardInputAdapter(pm, this.screen));
                            return false;
                        } else {
                            screen.log("What?");
                        }
                        break;
                    default:
                        screen.log("What?");
                        break;
                }

                Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));

                screen.finishTurn(currentX, currentY);

            } else if (keycode == Keys.BACKSPACE) {
                if (buffer.length() > 0) {
                    buffer.deleteCharAt(buffer.length() - 1);
                    screen.logDeleteLastChar();
                }
            } else if (keycode >= 29 && keycode <= 54) {
                buffer.append(Keys.toString(keycode).toUpperCase());
                screen.logAppend(Keys.toString(keycode).toUpperCase());
            }
            return false;
        }
    }

    private class InsertCardInputAdapter extends InputAdapter {

        GameScreen screen;
        PartyMember pm;

        public InsertCardInputAdapter(PartyMember pm, GameScreen screen) {
            this.screen = screen;
            this.pm = pm;
        }

        @Override
        public boolean keyUp(int keycode) {

            try {

                if (keycode == Keys.D) {
                    insertCard(0, 33, 0x8);
                } else if (keycode == Keys.S) {
                    insertCard(1, 31, 0x2);
                } else if (keycode == Keys.L) {
                    insertCard(3, 30, 0x1);
                } else if (keycode == Keys.M) {
                    insertCard(2, 32, 0x4);
                } else {
                    screen.log("What?");
                }

                Gdx.input.setInputProcessor(new InputMultiplexer(screen, stage));
                screen.finishTurn(currentX, currentY);

            } catch (PartyDeathException e) {
                screen.partyDeath();
            }

            return false;
        }

        private void insertCard(int idx, int x, int mask) throws PartyDeathException {
            if (pm.getPlayer().cards[idx] > 0) {
                pm.getPlayer().cards[idx]--;
                if (currentX == x && currentY == 12) {
                    this.screen.context.getParty().getSaveGame().exodusCardsStatus |= mask;
                    Sounds.play(Sound.DIVINE_MEDITATION);
                    animateExodusDeath(x, 11);
                } else {
                    Sounds.play(Sound.SPIRITS);
                    pm.applyDamage(pm.getPlayer().health + 1, false);
                }
            } else {
                screen.log("None owned!");
            }
        }

        private void animateExodusDeath(final int x, final int y) {

            Actor d = new Exodus.ExplosionLargeDrawable();
            Vector3 v = screen.getMapPixelCoords(x, y);
            d.setX(v.x - 72);
            d.setY(v.y - 72);

            SequenceAction seq = Actions.action(SequenceAction.class);
            seq.addAction(Actions.run(new AddActorAction(screen.projectilesStage, d)));
            seq.addAction(Actions.run(new PlaySoundAction(Sound.DIVINE_MEDITATION)));
            seq.addAction(Actions.delay(2f));
            seq.addAction(Actions.removeActor(d));
            seq.addAction(new Action() {
                @Override
                public boolean act(float delta) {
                    screen.replaceTile("lava", x, y);
                    return true;
                }
            });

            SaveGame sg = this.screen.context.getParty().getSaveGame();
            if ((sg.exodusCardsStatus & 0x1) > 0 && (sg.exodusCardsStatus & 0x2) > 0 && (sg.exodusCardsStatus & 0x4) > 0 && (sg.exodusCardsStatus & 0x8) > 0) {
                seq.addAction(new Action() {
                    @Override
                    public boolean act(float delta) {
                        Exodus.mainGame.setScreen(new FinalScreen(sg));
                        return true;
                    }
                });
            }

            screen.projectilesStage.addAction(seq);
        }

    }

}
