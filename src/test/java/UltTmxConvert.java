
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import objects.BaseMap;
import objects.MapSet;
import objects.Person;
import objects.Tile;
import objects.TileSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import java.io.FileInputStream;
import java.util.List;
import exodus.Constants;
import exodus.Constants.DungeonTile;
import exodus.Constants.MapType;
import exodus.Constants.Maps;
import exodus.Constants.ObjectMovementBehavior;
import java.util.ArrayList;

public class UltTmxConvert implements ApplicationListener {

    public static void main(String[] args) throws Exception {

        new LwjglApplication(new UltTmxConvert());
    }

    @Override
    public void create() {

        try {

            File file2 = new File("target/classes/assets/xml/tileset-base.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(TileSet.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            TileSet ts = (TileSet) jaxbUnmarshaller.unmarshal(file2);
            ts.setMaps();

            File file3 = new File("target/classes/assets/xml/maps.xml");
            jaxbContext = JAXBContext.newInstance(MapSet.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            MapSet ms = (MapSet) jaxbUnmarshaller.unmarshal(file3);
            ms.init(ts);

            //load the atlas and determine the tile indexes per tilemap position
            FileHandle f = new FileHandle("target/classes/assets/graphics/latest-atlas.txt");
            TextureAtlasData atlas = new TextureAtlasData(f, f.parent(), false);
            int png_grid_width = 24;
            Tile[] mapTileIds = new Tile[png_grid_width * Constants.tilePixelWidth + 1];
            for (Region r : atlas.getRegions()) {
                int x = r.left / r.width;
                int y = r.top / r.height;
                int i = x + (y * png_grid_width) + 1;
                mapTileIds[i] = ts.getTileByName(r.name);
            }

            for (BaseMap map : ms.getMaps()) {

                if (!map.getFname().endsWith("ult") || map.getId() == Maps.SOSARIA.getId()) {
                    continue;
                }

                FileInputStream is = new FileInputStream("target/classes/assets/data/" + map.getFname());
                byte[] bytes = IOUtils.toByteArray(is);

                Tile[] tiles = new Tile[map.getWidth() * map.getHeight()];
                int pos = 0;
                for (int y = 0; y < map.getHeight(); y++) {
                    for (int x = 0; x < map.getWidth(); x++) {
                        int index = (bytes[pos] & 0xff) / 4;
                        pos++;
                        Tile tile = ts.getTileByIndex(index);
                        if (tile == null) {
                            System.out.println("Tile index cannot be found: " + index + " using index 37 for black space.");
                            tile = ts.getTileByIndex(37);
                        }
                        tiles[x + y * map.getWidth()] = tile;
                    }
                }

                Formatter formatter = null;

                if (map.getType() == MapType.dungeon) {
                    String[] csv = new String[8];
                    pos = 0;
                    for (int lvl = 0; lvl < map.getLevels(); lvl++) {
                        int count = 1;
                        StringBuilder data = new StringBuilder();
                        for (int y = 0; y < map.getHeight(); y++) {
                            for (int x = 0; x < map.getWidth(); x++) {
                                byte idx = bytes[pos];
                                DungeonTile dt = DungeonTile.getTileByValue(idx);
                                pos++;
                                Tile tile = ts.getTileByName(dt.getTileName());
                                if (tile == null) {
                                    System.out.println("Tile index cannot be found: " + dt.getTileName() + " using index 37 for black space.");
                                    tile = ts.getTileByIndex(37);
                                }
                                data.append(findTileMapId(mapTileIds, tile.getName())).append(",");
                                count++;
                                if (count > 16) {
                                    data.append("\n");
                                    count = 1;
                                }
                            }
                        }
                        String d = data.toString();
                        d = d.substring(0, d.length() - 2);
                        csv[lvl] = d;
                    }
                    
                    pos = 0x800;
                    int[] textOffsets = new int[8];
                    for (int x = 0; x < 8; x++) {
                        textOffsets[x] = (bytes[pos] & 0xff) + 0x800;
                        pos += 2;
                    }

                    String[] texts = new String[8];
                    for (int x = 0; x < 8; x++) {
                        int os = textOffsets[x];
                        byte[] b = new byte[64];
                        int c = 0;
                        while (true) {
                            b[c] = bytes[os];
                            if (b[c] == 0) {
                                break;
                            }
                            os++;
                            c++;
                        }
                        texts[x] = new String(b, "UTF-8").trim();
                        texts[x] = texts[x].replaceAll("[<>]", "");
                        texts[x] = texts[x].replaceAll("[\n\r]", " ");

                    }

                    formatter = new Formatter(map.getFname(), "latest.png", map.getWidth(), map.getHeight(),
                            Constants.tilePixelWidth, Constants.tilePixelWidth, csv, texts);

                    String tmxFName = String.format("tmx/map_%s_%s.tmx", map.getId(), map.getFname().replace(".ult", ""));
                    FileUtils.writeStringToFile(new File(tmxFName), formatter.toDungeonString());
                    System.out.printf("Wrote: %s\n", tmxFName);

                } else {

                    //doors
                    for (int y = 0; y < map.getHeight(); y++) {
                        for (int x = 0; x < map.getWidth(); x++) {
                            Tile tile = tiles[x + y * map.getWidth()];
                            Tile left = x > 0 ? tiles[(x - 1) + y * map.getWidth()] : null;
                            Tile right = x < map.getWidth() - 1 ? tiles[(x + 1) + y * map.getWidth()] : null;
                            if (tile.getIndex() == 46 && (left != null && left.getRule() != Constants.TileRule.signs) && (right != null && right.getRule() != Constants.TileRule.signs)) {
                                tiles[x + y * map.getWidth()] = ts.getTileByName("locked_door");
                            }
                        }
                    }

                    //ambrosia doors
                    if (map.getId() == Maps.AMBROSIA.getId()) {
                        tiles[34 + 5 * map.getWidth()] = ts.getTileByName("locked_door");
                        tiles[35 + 5 * map.getWidth()] = ts.getTileByName("locked_door");
                        tiles[36 + 5 * map.getWidth()] = ts.getTileByName("locked_door");
                    }

                    List<Person> people = new ArrayList<>();

                    pos = 0x1180;
                    for (int x = 0; x < 32; x++) {
                        int index = (bytes[pos] & 0xff) / 4;
                        pos++;
                        Tile tile = ts.getTileByIndex(index);
                        if (tile == null) {
                            System.out.println("Tile index cannot be found: " + index + " using index 37 for black space.");
                            tile = ts.getTileByIndex(37);
                        }
                        Person p = new Person();
                        p.setTile(tile);
                        people.add(p);
                    }

                    pos = 0x11C0;
                    for (int x = 0; x < 32; x++) {
                        int dx = bytes[pos] & 0xff;
                        pos++;
                        people.get(x).setStart_x(dx);
                        people.get(x).setX(dx);
                    }

                    pos = 0x11E0;
                    for (int x = 0; x < 32; x++) {
                        int dy = bytes[pos] & 0xff;
                        pos++;
                        people.get(x).setStart_y(dy);
                        people.get(x).setY(dy);
                    }

                    pos = 0x1200;
                    for (int x = 0; x < 32; x++) {
                        int dialog = bytes[pos] & 0x0f;
                        int m = (bytes[pos] >> 4) & 0x0f;
                        ObjectMovementBehavior move = ObjectMovementBehavior.FIXED;
                        if (m == 4) {
                            move = ObjectMovementBehavior.WANDER;
                        }
                        if (m == 8) {
                            move = ObjectMovementBehavior.FOLLOW_AVATAR;
                        }
                        if (m == 12) {
                            move = ObjectMovementBehavior.ATTACK_AVATAR;
                            //System.out.println(people.get(x).getTile().getName() + " attacks avatar.");
                        }
                        pos++;
                        people.get(x).setMovement(move);
                        people.get(x).setDialogId(dialog);
                    }

                    StringBuilder peopleBuffer = new StringBuilder();
                    for (int y = 0; y < map.getHeight(); y++) {
                        for (int x = 0; x < map.getWidth(); x++) {
                            Person p = findPersonAtCoords(people, x, y);
                            if (p == null || p.getTile().getIndex() == 0) {
                                peopleBuffer.append("0,");
                            } else {
                                peopleBuffer.append(findTileMapId(mapTileIds, p.getTile().getName())).append(",");
                            }
                        }
                        peopleBuffer.append("\n");
                    }

                    int count = 1;
                    String p = peopleBuffer.toString();
                    if (p == null || p.length() < 1) {
                        count = 1;
                        //make empty
                        for (int i = 0; i < map.getWidth() * map.getHeight(); i++) {
                            peopleBuffer.append("0,");
                            count++;
                            if (count > map.getWidth()) {
                                peopleBuffer.append("\n");
                                count = 1;
                            }
                        }
                        p = peopleBuffer.toString();
                    }
                    p = p.substring(0, p.length() - 2);

                    pos = 0x11A0;
                    for (int x = 0; x < 32; x++) {
                        int index = (bytes[pos] & 0xff) / 4;
                        pos++;
                        Tile tile = ts.getTileByIndex(index);
                        if (tile == null) {
                            System.out.println("Tile index cannot be found: " + index + " using index 37 for black space.");
                            tile = ts.getTileByIndex(37);
                        }
                        int dx = people.get(x).getX();
                        int dy = people.get(x).getY();

                        tiles[dx + dy * map.getWidth()] = tile;
                    }

                    pos = 0x1000;
                    int[] textOffsets = new int[8];
                    for (int x = 0; x < 8; x++) {
                        textOffsets[x] = (bytes[pos] & 0xff) + 0x1000;
                        pos += 2;
                    }

                    String[] texts = new String[8];
                    for (int x = 0; x < 8; x++) {
                        int os = textOffsets[x];
                        byte[] b = new byte[64];
                        int c = 0;
                        while (true) {
                            b[c] = bytes[os];
                            if (b[c] == 0) {
                                break;
                            }
                            os++;
                            c++;
                        }
                        texts[x] = new String(b, "UTF-8").trim();
                        texts[x] = texts[x].replaceAll("[<>]", "");
                        texts[x] = texts[x].replaceAll("[\n\r]", " ");

                    }

                    for (int x = 0; x < 8; x++) {
                        for (Person per : people) {
                            if (per.getDialogId() == x + 1) {
                                per.setConversation(texts[x]);
                            }
                        }
                    }

                    //map layer
                    StringBuilder data = new StringBuilder();
                    count = 1;
                    for (int i = 0; i < tiles.length; i++) {
                        Tile t = tiles[i];
                        data.append(findTileMapId(mapTileIds, t.getName())).append(",");
                        count++;
                        if (count > 32) {
                            data.append("\n");
                            count = 1;
                        }
                    }

                    String d = data.toString();
                    d = d.substring(0, d.length() - 2);

                    formatter = new Formatter(map.getFname(), "latest.png", map.getWidth(), map.getHeight(),
                            Constants.tilePixelWidth, Constants.tilePixelWidth, d, p, people);

                    String tmxFName = String.format("tmx/map_%s_%s.tmx", map.getId(), map.getFname().replace(".ult", ""));
                    FileUtils.writeStringToFile(new File(tmxFName), formatter.toString());
                    System.out.printf("Wrote: %s\n", tmxFName);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DONE");
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void render() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }

    private int findTileMapId(Tile[] tiles, String name) {
        for (int i = 1; i < tiles.length; i++) {
            if (tiles[i] == null) {
                continue;
            }
            if (StringUtils.equals(tiles[i].getName(), name)) {
                return i;
            }
        }
        return 0;
    }

    private Person findPersonAtCoords(List<Person> people, int x, int y) {
        for (Person p : people) {
            if (p != null && (p.getStart_x() == x && p.getStart_y() == y)) {
                return p;
            }
        }
        return null;
    }

    private static class Formatter {

        private String tilesetName;
        private String imageSource;
        private int mapWidth;
        private int mapHeight;
        private int tileWidth;
        private int tileHeight;
        private String data;
        private String[] dungdata;
        private String[] writings;
        private String people;
        private List<Person> persons;

        public Formatter(String tilesetName, String imageSource, int mapWidth, int mapHeight, int tileWidth, int tileHeight, String data, String people, List<Person> persons) {
            this.tilesetName = tilesetName;
            this.imageSource = imageSource;
            this.mapWidth = mapWidth;
            this.mapHeight = mapHeight;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.data = data;
            this.people = people;
            this.persons = persons;
        }

        public Formatter(String tilesetName, String imageSource, int mapWidth, int mapHeight, int tileWidth, int tileHeight, String[] data, String[] writings) {
            this.tilesetName = tilesetName;
            this.imageSource = imageSource;
            this.mapWidth = mapWidth;
            this.mapHeight = mapHeight;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.dungdata = data;
            this.writings = writings;
        }

        @Override
        public String toString() {

            StringBuilder personsString = new StringBuilder();
            if (persons != null) {
                for (Person p : persons) {
                    if (p == null || p.getTile().getIndex() == 0) {
                        continue;
                    }
                    personsString.append(p.toTMXString());
                }
            }

            String template = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<map version=\"1.0\" orientation=\"orthogonal\" width=\"%s\" height=\"%s\" tilewidth=\"%s\" tileheight=\"%s\" backgroundcolor=\"#000000\">\n"
                    + "<tileset firstgid=\"1\" name=\"%s\" tilewidth=\"%s\" tileheight=\"%s\">\n"
                    + "<image source=\"%s\" width=\"768\" height=\"768\"/>\n</tileset>\n"
                    + "<layer name=\"Map Layer\" width=\"%s\" height=\"%s\">\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"People Layer\" width=\"%s\" height=\"%s\">\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<objectgroup name=\"Person Properties\" width=\"%s\" height=\"%s\">\n%s\n</objectgroup>\n"
                    + "</map>";

            return String.format(template, mapWidth, mapHeight, tileWidth, tileHeight,
                    tilesetName, tileWidth, tileHeight,
                    imageSource,
                    mapWidth, mapHeight, data,
                    mapWidth, mapHeight, people,
                    mapWidth, mapHeight, personsString.toString()
            );

        }

        public String toDungeonString() {

            String template = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<map version=\"1.0\" orientation=\"orthogonal\" width=\"%s\" height=\"%s\" tilewidth=\"%s\" tileheight=\"%s\" backgroundcolor=\"#000000\">\n"
                    + "<tileset firstgid=\"1\" name=\"%s\" tilewidth=\"%s\" tileheight=\"%s\">\n"
                    + "<image source=\"%s\" width=\"768\" height=\"768\"/>\n</tileset>\n"
                    + "<layer name=\"Dungeon Level 1\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 2\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 3\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 4\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 5\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 6\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 7\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "<layer name=\"Dungeon Level 8\" width=\"%s\" height=\"%s\">\n<properties><property name=\"writings\" value=\"%s\"/></properties>\n"
                    + "<data encoding=\"csv\">\n%s\n</data>\n</layer>\n"
                    + "</map>";

            return String.format(template,
                    mapWidth, mapHeight,
                    tileWidth, tileHeight,
                    tilesetName, tileWidth, tileHeight,
                    imageSource,
                    mapWidth, mapHeight, writings[0],dungdata[0],
                    mapWidth, mapHeight, writings[1],dungdata[1],
                    mapWidth, mapHeight, writings[2],dungdata[2],
                    mapWidth, mapHeight, writings[3],dungdata[3],
                    mapWidth, mapHeight, writings[4],dungdata[4],
                    mapWidth, mapHeight, writings[5],dungdata[5],
                    mapWidth, mapHeight, writings[6],dungdata[6],
                    mapWidth, mapHeight, writings[7],dungdata[7]
            );

        }
    }
}
