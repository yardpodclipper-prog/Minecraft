package com.yourname.gtstracker.database.models;

public class IVStats {
    private int hp;
    private int atk;
    private int def;
    private int spatk;
    private int spdef;
    private int speed;

    public IVStats(int hp, int atk, int def, int spatk, int spdef, int speed) {
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.spatk = spatk;
        this.spdef = spdef;
        this.speed = speed;
    }

    public int getTotal() {
        return hp + atk + def + spatk + spdef + speed;
    }

    public boolean isPerfect() {
        return getTotal() == 186;
    }

    public int getHp() { return hp; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getSpatk() { return spatk; }
    public int getSpdef() { return spdef; }
    public int getSpeed() { return speed; }
}
