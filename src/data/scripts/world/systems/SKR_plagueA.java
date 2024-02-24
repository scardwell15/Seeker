package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;

import java.awt.*;

import static data.scripts.util.SKR_txt.txt;

public class SKR_plagueA {

    public void generate(SectorAPI sector, Integer direction) {
        String systemName = txt("plague_A" + MathUtils.getRandomNumberInRange(0, 9));

        StarSystemAPI system = sector.createStarSystem(systemName);

        system.setEnteredByPlayer(false);
        system.getTags().add(Tags.THEME_HIDDEN);
        system.getTags().add(Tags.THEME_UNSAFE);
        system.getTags().add("theme_plaguebearer");

        system.setBackgroundTextureFilename("graphics/SEEKER/backgrounds/SKR_plagueA.png");

        // create the star and generate the hyperspace anchor for this system
        PlanetAPI star = system.initStar("plague_" + systemName, // unique id for this star
                "star_browndwarf", // id in planets.json
                300f,
                50);        // radius (in pixels at default zoom)
        system.setLightColor(new Color(200, 125, 75)); // light color in entire system, affects all entities


//        final HyperspaceTerrainPlugin hyper = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
//        final int[][] cells = hyper.getTiles();
//        final float cellSize = hyper.getTileSize();
//        
//	system.getLocation().set(
//                MathUtils.getPointOnCircumference(
//                        new Vector2f(),
//                        MathUtils.getRandomNumberInRange(20000, Math.min(cells.length, cells[0].length)*cellSize*0.6f),
//                        MathUtils.getRandomNumberInRange(-120, -150)
//                )
//        );

        //simpler random location

        system.getLocation().set(
                MathUtils.getPointOnCircumference(
                        new Vector2f(),
                        MathUtils.getRandomNumberInRange(20000, 50000),
                        MathUtils.getRandomNumberInRange(direction - 45, direction + 45)
                )
        );

        PlanetAPI p1 = system.addPlanet(
                "plagueA_1",
                star,
                txt("plague_A_planet1"),
                "desert",
                -120,
                125,
                900,
                200
        );
        p1.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);
        p1.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
        p1.getMarket().addCondition(Conditions.ORE_MODERATE);
        p1.getMarket().addCondition(Conditions.HOT);
        p1.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);

        MagicCampaign.addJumpPoint("plagueA_jp1", systemName + txt("plague_jp"), p1, star, 0, 900, 200);

        SectorEntityToken s1 = system.addCustomEntity("plagueA_stable1", null, "stable_location", "neutral");
        s1.setCircularOrbit(star, -240, 900, 200);

        PlanetAPI p2 = system.addPlanet(
                "plagueA_2",
                star,
                txt("plague_A_planet2"),
                "gas_giant",
                120,
                250,
                2000,
                600
        );
        system.addAsteroidBelt(p2, 50, 450, 50, 30, 40);
        system.addRingBand(p2, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 450, 35, null, null);
        system.addPlanet(
                "plagueA_2a",
                p2,
                txt("plague_A_planet2a"),
                "barren",
                60,
                40,
                550,
                75
        );

        SectorEntityToken fieldA = MagicCampaign.createDebrisField(
                "plagueA_debris1",
                250,
                2f,
                10000,
                0,
                250,
                -1,
                null,
                -1,
                0.2f,
                true,
                null,
                p2,
                90,
                500,
                70
        );

        MagicCampaign.addSalvage(null, fieldA, MagicCampaign.lootType.SUPPLIES, null, 231);

        PersonAPI plagueA = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.GAMMA_CORE)
                .setFirstName("Safeguard")
                .setLastName("Safeguard")
                .setPortraitId("SKR_plagueA")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.AGGRESSIVE)
                .setLevel(10)
                .setEliteSkillsOverride(4)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_NO_DEFENSE)
                .create();

        SectorEntityToken boss = MagicCampaign.createFleetBuilder()
                .setFleetName("plague_A_fleet")
                .setFleetFaction("plague")
                .setFlagshipName("plague_A_boss")
                .setFlagshipVariant("SKR_keep_safeguard")
                .setCaptain(plagueA)
                .setMinFP(200)
                .setReinforcementFaction(Factions.DERELICT)
                .setQualityOverride(0.5f)
                .setAssignment(FleetAssignment.ORBIT_PASSIVE)
                .setAssignmentTarget(p2)
                .setIsImportant(true)
                .setTransponderOn(false)
                .create();
        boss.getCargo().addCommodity(Commodities.ALPHA_CORE, 1);
        boss.getCargo().addHullmods("SKR_plagueLPC", 1);
        boss.setDiscoverable(true);

        SectorEntityToken relay = system.addCustomEntity("plagueA_relay",
                null, // name - if null, defaultName from custom_entities.json will be used
                "comm_relay", // type of object, defined in custom_entities.json
                "neutral"); // faction
        relay.setCircularOrbitPointingDown(star, 270 - 60, 4500, 1250);

        //random stuff to the outside

        StarSystemGenerator.addOrbitingEntities(system, star, StarAge.OLD,
                4, 6, // min/max entities to add
                2750, // radius to start adding at
                0, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                true); // whether to use custom or system-name based names

        system.autogenerateHyperspaceJumpPoints(true, true, true);

        MagicCampaign.hyperspaceCleanup(system);
    }
}