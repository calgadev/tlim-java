package com.tlim;

import com.tlim.character.Character;
import com.tlim.creature.Creature;
import com.tlim.inventory.Inventory;
import com.tlim.item.Item;
import com.tlim.item.NpcBuyer;
import com.tlim.server.PvpType;
import com.tlim.server.Server;
import com.tlim.user.User;

/** Small builders for in-memory entities used by service unit tests. */
public final class TestData {

    private TestData() {}

    public static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole("USER");
        return user;
    }

    public static Server server(long id, String name) {
        Server server = new Server();
        server.setId(id);
        server.setName(name);
        server.setPvpType(PvpType.OPTIONAL_PVP);
        return server;
    }

    public static Character character(long id, User owner, Server server) {
        Character character = new Character();
        character.setId(id);
        character.setName("Char " + id);
        character.setUser(owner);
        character.setServer(server);
        return character;
    }

    public static Item item(long id, String displayName) {
        Item item = new Item();
        item.setId(id);
        item.setName(displayName);
        item.setActualName(displayName.toLowerCase());
        return item;
    }

    public static Creature creature(long id, String displayName) {
        Creature creature = new Creature();
        creature.setId(id);
        creature.setName(displayName);
        creature.setActualName(displayName.toLowerCase());
        return creature;
    }

    public static Inventory inventory(Character character, Item item, int current, int target) {
        Inventory inventory = new Inventory();
        inventory.setCharacter(character);
        inventory.setItem(item);
        inventory.setCurrentQuantity(current);
        inventory.setTargetQuantity(target);
        return inventory;
    }

    public static NpcBuyer buyer(Item item, String npc, int price) {
        NpcBuyer buyer = new NpcBuyer();
        buyer.setItem(item);
        buyer.setNpcName(npc);
        buyer.setPrice(price);
        return buyer;
    }
}
