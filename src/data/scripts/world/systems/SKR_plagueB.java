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

public class SKR_plagueB {

    public void generate(SectorAPI sector, Integer direction) {
        String systemName = txt("plague_B" + MathUtils.getRandomNumberInRange(0, 9));

        StarSystemAPI system = sector.createStarSystem(systemName);
        system.setEnteredByPlayer(false);
        system.getTags().add(Tags.THEME_HIDDEN);
        system.getTags().add(Tags.THEME_UNSAFE);
        system.getTags().add("theme_plaguebearer");

        system.setBackgroundTextureFilename("graphics/SEEKER/backgrounds/SKR_plagueB.png");

        // create the star and generate the hyperspace anchor for this system
        PlanetAPI star = system.initStar("plague_" + systemName, // unique id for this star
                "star_white", // id in planets.json
                150f,
                150);        // radius (in pixels at default zoom)
        system.setLightColor(new Color(75, 75, 150)); // light color in entire system, affects all entities


        //simpler random location

        system.getLocation().set(
                MathUtils.getPointOnCircumference(
                        new Vector2f(),
                        MathUtils.getRandomNumberInRange(20000, 50000),
                        MathUtils.getRandomNumberInRange(direction - 45, direction + 45)
                )
        );

        //research station
        SectorEntityToken research1 = system.addCustomEntity("plagueB_research", null, "station_research", "neutral");
        research1.setCircularOrbitPointingDown(star, 180, 750, 90);
        research1.setDiscoverable(true);

        //remote planet
        PlanetAPI p1 = system.addPlanet(
                "plagueB_1",
                star,
                txt("plague_B_planet1"),
                "barren",
                0,
                225,
                2500,
                190
        );
        p1.getMarket().addCondition(Conditions.ORE_SPARSE);
        p1.getMarket().addCondition(Conditions.RARE_ORE_ULTRARICH);
        p1.getMarket().addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);
        p1.getMarket().addCondition(Conditions.DARK);
        p1.getMarket().addCondition(Conditions.COLD);
        p1.getMarket().addCondition(Conditions.HIGH_GRAVITY);

        //jump point from planet
        MagicCampaign.addJumpPoint("plagueB_jp1", systemName + txt("plague_jp"), p1, star, 60, 2500, 300);

        //2 stable locations
        SectorEntityToken s1 = system.addCustomEntity("plagueB_stable1", null, "stable_location", "neutral");
        s1.setCircularOrbit(star, -240, 2900, 220);
        SectorEntityToken s2 = system.addCustomEntity("plagueB_stable2", null, "stable_location", "neutral");
        s2.setCircularOrbit(star, 60, 2900, 220);

        PersonAPI plagueB = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setFirstName("Rampage")
                .setLastName("Rampage")
                .setPortraitId("SKR_plagueB")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.RECKLESS)
                .setLevel(10)
                .setEliteSkillsOverride(6)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE)
                .create();

        SectorEntityToken boss = MagicCampaign.createFleetBuilder()
                .setFleetName("plague_B_fleet")
                .setFleetFaction("plague")
                .setFlagshipName("plague_B_boss")
                .setFlagshipVariant("SKR_rampage_01")
                .setCaptain(plagueB)
                .setAssignment(FleetAssignment.ORBIT_PASSIVE)
                .setAssignmentTarget(p1)
                .setIsImportant(true)
                .setTransponderOn(false)
                .create();
        boss.getCargo().addCommodity(Commodities.ALPHA_CORE, 2);
        boss.getCargo().addHullmods("SKR_plagueLPC", 1);
        boss.setDiscoverable(true);

        //random stuff to the outside

        StarSystemGenerator.addOrbitingEntities(system, star, StarAge.OLD,
                4, 6, // min/max entities to add
                3000, // radius to start adding at
                0, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                true); // whether to use custom or system-name based names

        system.autogenerateHyperspaceJumpPoints(true, true, true);

        MagicCampaign.hyperspaceCleanup(system);
    }
}