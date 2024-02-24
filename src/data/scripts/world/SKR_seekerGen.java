package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import data.campaign.bosses.SKR_cataclysmLoot;
import data.campaign.bosses.SKR_rampageLoot;
import data.campaign.bosses.SKR_safeguardLoot;
import data.campaign.bosses.SKR_whiteDwarfLoot;
import data.campaign.ids.SKR_ids;
import data.scripts.world.systems.SKR_plagueA;
import data.scripts.world.systems.SKR_plagueB;
import data.scripts.world.systems.SKR_plagueC;
import data.scripts.world.systems.SKR_plagueD;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;

import java.util.*;

import static data.scripts.util.SKR_txt.txt;

public class SKR_seekerGen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {

        List<Integer> directions = new ArrayList<>();
        directions.add(45);
        directions.add(-45);
        directions.add(135);
        directions.add(-135);
        Collections.shuffle(directions);

        new SKR_plagueA().generate(sector, directions.get(0));
        new SKR_plagueB().generate(sector, directions.get(1));
        new SKR_plagueC().generate(sector, directions.get(2));
        new SKR_plagueD().generate(sector, directions.get(3));

        initFactionRelationships(sector);
    }

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI plague = sector.getFaction("plague");
        for (FactionAPI f : sector.getAllFactions()) {
            plague.setRelationship(f.getId(), RepLevel.INHOSPITABLE);
        }
        plague.setRelationship(Factions.REMNANTS, RepLevel.FRIENDLY);
        plague.setRelationship(Factions.DERELICT, RepLevel.FRIENDLY);
        plague.setRelationship(Factions.NEUTRAL, RepLevel.NEUTRAL);
    }

    private static final Logger LOG = Global.getLogger(SKR_seekerGen.class);

    public static void addExplorationContent() {
        spawnStellar();

        spawnSafeguard();
        spawnRampage();
        spawnWhiteDwarf();
        spawnCataclysm();

        spawnDemeter();
        spawnTitanic();
        spawnVoulge();
    }

    ////////////////////
    //                //
    //    STELLAR     //
    //                //
    ////////////////////

    private static void spawnStellar() {

        //find all Nexus
        List<CampaignFleetAPI> stations = new ArrayList<>();

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_REMNANT)) {
                for (CampaignFleetAPI fleet : system.getFleets()) {
                    if (fleet.isStationMode()) {
                        if (fleet.getFlagship().getVariant().getHullVariantId().equals("remnant_station2_Standard")) {
                            stations.add(fleet);
                        }
                    }
                }
            }
        }

//        for(LocationAPI location : Global.getSector().getStarSystems()){
//            if(!location.isHyperspace() && !location.getFleets().isEmpty()){
//                for(CampaignFleetAPI fleet : location.getFleets()){
//                    if (fleet.isStationMode() ){
//                        if( fleet.getFlagship().getVariant().getHullVariantId().equals("remnant_station2_Standard")){
//                            stations.add(fleet);
//                        }
//                    }
//                }
//            }
//        }

        if (stations.isEmpty()) {
            LOG.info("No suitable system found to spawn STELLAR.");
        } else {
            //try to avoid HMI systems
            List<String> blacklist = new ArrayList<>();
            {
                blacklist.add(Tags.THEME_HIDDEN);
                blacklist.add("theme_plaguebearers");
                blacklist.add("theme_domres");
                blacklist.add("theme_hmi_nightmare");
                blacklist.add("theme_hmi_mess_remnant");
                blacklist.add("theme_messrem");
                blacklist.add("theme_domresboss");
                blacklist.add("theme_domres");
            }

            //pick closest to the core
            float distance = 999999999;
            CampaignFleetAPI selected = null;
            CampaignFleetAPI backup = stations.get(0);
            for (CampaignFleetAPI station : stations) {
                for (String theme : blacklist) {
                    if (!station.getStarSystem().hasTag(theme)) {
                        //try to avoid blackholes and pulsars but keep as a backup
                        if (station.getStarSystem().getStar().getSpec().isPulsar() || station.getStarSystem().getStar().getSpec().isBlackHole()) {
                            float dist = MathUtils.getDistanceSquared(station.getStarSystem().getLocation(), new Vector2f());
                            if (dist < distance) {
                                distance = dist;
                                backup = station;
                            }
                        } else {
                            float dist = MathUtils.getDistanceSquared(station.getStarSystem().getLocation(), new Vector2f());
                            if (dist < distance) {
                                distance = dist;
                                selected = station;
                            }
                        }
                    }
                }
            }
            //switch to backup if needed
            if (selected == null) selected = backup;

            LOG.info("Adding STELLAR boss fleet in " + selected.getStarSystem().getName());

            //add cool derelict to salvage
            SectorEntityToken onyx = MagicCampaign.createDerelict(
                    "SKR_onyx_night",
                    ShipRecoverySpecial.ShipCondition.WRECKED,
                    true,
                    -1,
                    true,
                    (SectorEntityToken) selected,
                    MathUtils.getRandomNumberInRange(0, 360),
                    MathUtils.getRandomNumberInRange(50, 150),
                    MathUtils.getRandomNumberInRange(30, 50)
            );
            onyx.addTag(Tags.NEUTRINO_LOW);

            MagicCampaign.addSalvage(null, onyx, MagicCampaign.lootType.WEAPON, "SKR_blackout", 2);

            //add STELLAR fleet

            CampaignFleetAPI stellar = MagicCampaign.createFleetBuilder()
                    .setFleetName(txt("stellar_fleet"))
                    .setFleetFaction(Factions.REMNANTS)
                    .setFleetType(FleetTypes.PERSON_BOUNTY_FLEET)
                    .setFlagshipName(txt("stellar_boss"))
                    .setFlagshipVariant("SKR_stellar_falseOmega")
                    .setFlagshipAutofit(true)
                    .setSupportAutofit(true)
                    .setMinFP(200)
                    .setQualityOverride(2f)
                    .setSpawnLocation(selected)
                    .setAssignment(FleetAssignment.DEFEND_LOCATION)
                    .setAssignmentTarget(onyx)
                    .setIsImportant(false)
                    .setTransponderOn(true)
                    .create();
            stellar.addTag(Tags.NEUTRINO);
            stellar.addTag(Tags.NEUTRINO_HIGH);
            stellar.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
            stellar.getFlagship().getVariant().addTag(Tags.SHIP_UNIQUE_SIGNATURE);
            stellar.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
            Global.getSector().getMemoryWithoutUpdate().set("$SKR_stellar", false);

//            selected.getStarSystem().getTags().add("theme_plaguebearers");
        }
    }

    ////////////////////
    //                //
    //   SAFEGUARD    //
    //                //
    ////////////////////

    private static void spawnSafeguard() {

        List<String> themesPlagueA = new ArrayList<>();
        themesPlagueA.add(Tags.THEME_DERELICT_PROBES);
        themesPlagueA.add(Tags.THEME_DERELICT_SURVEY_SHIP);

        List<String> notThemesPlagueA = new ArrayList<>();
        notThemesPlagueA.add(SKR_ids.THEME_PLAGUEBEARER);
        notThemesPlagueA.add(Tags.THEME_HIDDEN);
        notThemesPlagueA.add("theme_already_occupied");
        notThemesPlagueA.add("theme_already_colonized");
        notThemesPlagueA.add("no_pulsar_blackhole");

        List<String> entitiesPlagueA = new ArrayList<>();
        entitiesPlagueA.add(Tags.GAS_GIANT);

        SectorEntityToken targetPlagueA = MagicCampaign.findSuitableTarget(
                null,
                null,
                "CLOSE",
                themesPlagueA,
                notThemesPlagueA,
                entitiesPlagueA,
                true,
                true,
                true
        );

        if (targetPlagueA == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR SAFEGUARD");
            return;
        } else {
            LOG.info("Adding SAFEGUARD boss fleet in " + targetPlagueA.getStarSystem().getName());
        }

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
                targetPlagueA,
                90,
                500,
                70
        );

        MagicCampaign.addSalvage(null, fieldA, MagicCampaign.lootType.SUPPLIES, null, 231);

        PersonAPI plagueA = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.GAMMA_CORE)
                .setFirstName(txt("plague_A_boss"))
                .setLastName(txt("plague_A_boss"))
                .setPortraitId("SKR_plagueA")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.RECKLESS)
                .setLevel(6)
                .setEliteSkillsOverride(2)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_NO_DEFENSE)
                .create();

        CampaignFleetAPI safeguard = MagicCampaign.createFleetBuilder()
                .setFleetName(txt("plague_A_fleet"))
                .setFleetFaction("plague")
                .setFlagshipName(txt("plague_A_boss"))
                .setFlagshipVariant("SKR_keep_safeguard")
                .setCaptain(plagueA)
                .setSupportAutofit(true)
                .setMinFP(200)
                .setReinforcementFaction(Factions.DERELICT)
                .setQualityOverride(0.5f)
                .setAssignment(FleetAssignment.PATROL_SYSTEM)
                .setAssignmentTarget(targetPlagueA)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();
        safeguard.setDiscoverable(true);
        safeguard.addTag(Tags.NEUTRINO);
        safeguard.addTag(Tags.NEUTRINO_HIGH);
        safeguard.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
        safeguard.getFlagship().getVariant().addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        safeguard.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        Global.getSector().getMemoryWithoutUpdate().set("$SKR_safeguard_boss", true);
        safeguard.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        safeguard.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        safeguard.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);

        safeguard.addEventListener(new SKR_safeguardLoot());

        targetPlagueA.getStarSystem().getTags().add(Tags.THEME_UNSAFE);
        targetPlagueA.getStarSystem().getTags().add(SKR_ids.THEME_PLAGUEBEARER);
    }

    ////////////////////
    //                //
    //    RAMPAGE     //
    //                //
    ////////////////////

    private static void spawnRampage() {
        List<String> themesPlagueB = new ArrayList<>();
        themesPlagueB.add(Tags.THEME_RUINS);

        List<String> notThemesPlagueB = new ArrayList<>();
        notThemesPlagueB.add(SKR_ids.THEME_PLAGUEBEARER);
        notThemesPlagueB.add(Tags.THEME_HIDDEN);
        notThemesPlagueB.add("theme_already_occupied");
        notThemesPlagueB.add("theme_already_colonized");
        notThemesPlagueB.add("no_pulsar_blackhole");

        SectorEntityToken targetPlagueB = MagicCampaign.findSuitableTarget(
                null,
                null,
                "CLOSE",
                themesPlagueB,
                notThemesPlagueB,
                null,
                true,
                true,
                true
        );

        if (targetPlagueB == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR RAMPAGE");
            return;
        } else {
            LOG.info("Adding RAMPAGE boss fleet in " + targetPlagueB.getStarSystem().getName());
        }

        PersonAPI plagueB = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setFirstName(txt("plague_B_boss"))
                .setLastName(txt("plague_B_boss"))
                .setPortraitId("SKR_plagueB")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.AGGRESSIVE)
                .setLevel(10)
                .setEliteSkillsOverride(10)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE)
                .create();

        CampaignFleetAPI rampage = MagicCampaign.createFleetBuilder()
                .setFleetName(txt("plague_B_fleet"))
                .setFleetFaction("plague")
                .setFlagshipName(txt("plague_B_boss"))
                .setFlagshipVariant("SKR_rampage_01")
                .setCaptain(plagueB)
                .setAssignment(FleetAssignment.PATROL_SYSTEM)
                .setAssignmentTarget(targetPlagueB)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();
        rampage.setDiscoverable(true);
        rampage.addTag(Tags.NEUTRINO);
        rampage.addTag(Tags.NEUTRINO_HIGH);
        rampage.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
        rampage.getFlagship().getVariant().addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        rampage.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        Global.getSector().getMemoryWithoutUpdate().set("$SKR_rampage_boss", true);
        //rampage.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        rampage.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        rampage.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);

        rampage.addEventListener(new SKR_rampageLoot());

        //remove the ruins tag to prevent scavengers
        targetPlagueB.getStarSystem().getTags().remove(Tags.THEME_RUINS);
        targetPlagueB.getStarSystem().getTags().remove(Tags.THEME_RUINS_MAIN);
        targetPlagueB.getStarSystem().getTags().remove(Tags.THEME_RUINS_SECONDARY);
        targetPlagueB.getStarSystem().getTags().add(Tags.THEME_UNSAFE);
        targetPlagueB.getStarSystem().getTags().add(SKR_ids.THEME_PLAGUEBEARER);
    }

    ////////////////////
    //                //
    //   WHITE DWARF  //
    //                //
    ////////////////////

    private static void spawnWhiteDwarf() {
        List<String> themesPlagueC = new ArrayList<>();
        themesPlagueC.add(Tags.THEME_REMNANT_SECONDARY);
        themesPlagueC.add(Tags.THEME_REMNANT_RESURGENT);

        List<String> notThemesPlagueC = new ArrayList<>();
        notThemesPlagueC.add(SKR_ids.THEME_PLAGUEBEARER);
        notThemesPlagueC.add(Tags.THEME_HIDDEN);
        notThemesPlagueC.add("theme_already_colonized");
        notThemesPlagueC.add("no_pulsar_blackhole");

        List<String> entitiesPlagueC = new ArrayList<>();
        entitiesPlagueC.add(Tags.STATION);
        entitiesPlagueC.add(Tags.DEBRIS_FIELD);
        entitiesPlagueC.add(Tags.WRECK);
        entitiesPlagueC.add(Tags.SALVAGEABLE);

        SectorEntityToken targetPlagueC = null;
        for (int i = 0; i < 5; i++) {
            targetPlagueC = MagicCampaign.findSuitableTarget(
                    null,
                    null,
                    "FAR",
                    themesPlagueC,
                    notThemesPlagueC,
                    entitiesPlagueC,
                    false,
                    true,
                    true
            );
            if (targetPlagueC != null && targetPlagueC.getStarSystem().isProcgen()) {
                break;
            }
        }


        if (targetPlagueC == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR WHITE DWARF");
            return;
        } else {
            LOG.info("Adding WHITE DWARF boss fleet in " + targetPlagueC.getStarSystem().getName());
        }

        PersonAPI plagueC = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setFirstName(txt("plague_C_boss"))
                .setLastName(txt("plague_C_boss"))
                .setPortraitId("SKR_plagueC")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.AGGRESSIVE)
                .setLevel(12)
                .setEliteSkillsOverride(6)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE)
                .create();

//        Map<String,Integer> fleet = new HashMap<>();
//        fleet.put("brilliant_Standard", 1);
//        fleet.put("fulgent_Assault", 1);
//        fleet.put("fulgent_Support", 1);
//        fleet.put("scintilla_Strike", 1);
//        fleet.put("scintilla_Support", 1);

        CampaignFleetAPI whitedwarf = MagicCampaign.createFleetBuilder()
                .setFleetName(txt("plague_C_fleet"))
                .setFleetFaction("plague")
                .setFlagshipName(txt("plague_C_boss"))
                .setFlagshipVariant("SKR_whiteDwarf_1")
                .setCaptain(plagueC)
                .setSupportAutofit(true)
                .setMinFP(200)
                .setReinforcementFaction(Factions.REMNANTS)
                .setQualityOverride(1f)
                .setAssignment(FleetAssignment.PATROL_SYSTEM)
                .setAssignmentTarget(targetPlagueC)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();
        whitedwarf.setDiscoverable(true);
        whitedwarf.addTag(Tags.NEUTRINO);
        whitedwarf.addTag(Tags.NEUTRINO_HIGH);
        whitedwarf.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
        whitedwarf.getFlagship().getVariant().addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        whitedwarf.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        Global.getSector().getMemoryWithoutUpdate().set("$SKR_whitedwarf_boss", true);
        whitedwarf.getMemoryWithoutUpdate().set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        whitedwarf.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        whitedwarf.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);

        //add escort
        Map<String, Integer> flagships = new HashMap<>();
        flagships.put("fulgent_Assault", 3);
        flagships.put("scintilla_Strike", 4);
        flagships.put("brilliant_Standard", 5);
        flagships.put("SKR_fresnel_standard", 5);
        flagships.put("apex_Standard", 5);
        flagships.put("nova_Standard", 7);
        flagships.put("radiant_Standard", 7);

        List<String> flagshipVariants = new ArrayList<>(flagships.keySet());

        for (int i = 0; i < 5; i++) {
            String flagshipVariant = flagshipVariants.get(MathUtils.getRandomNumberInRange(0, flagships.size() - 1));
            int size = flagships.get(flagshipVariant);

            PersonAPI hackedCore = MagicCampaign.createCaptainBuilder("remnant")
                    .setIsAI(true)
                    .setAICoreType(Commodities.ALPHA_CORE)
                    .setFirstName(txt("plague_D_boss"))
                    .setLastName(txt("plague_AIcore"))
                    .setPortraitId("SKR_AIcore")
                    .setGender(FullName.Gender.ANY)
                    .setFactionId("remnant")
                    .setRankId(Ranks.SPACE_CAPTAIN)
                    .setPostId(Ranks.POST_PATROL_COMMANDER)
                    .setPersonality(Personalities.AGGRESSIVE)
                    .setLevel(2 + MathUtils.getRandomNumberInRange(2, size))
                    .setEliteSkillsOverride(2 + MathUtils.getRandomNumberInRange(1, size))
                    .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE)
                    .create();

            CampaignFleetAPI hackedFleet = MagicCampaign.createFleetBuilder()
                    .setFleetName(txt("plague_AIFleet"))
                    .setFleetFaction("remnant")
                    .setFleetType(FleetTypes.PATROL_MEDIUM)
                    .setFlagshipName(txt("plague_blank"))
                    .setFlagshipVariant(flagshipVariant)
                    .setCaptain(hackedCore)
                    .setMinFP((1 + size) * 35)
                    .setReinforcementFaction("remnant")
                    .setQualityOverride(1f)
                    .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
                    .setAssignmentTarget(whitedwarf)
                    .setIsImportant(false)
                    .setTransponderOn(true)
                    .create();
        }


        whitedwarf.addEventListener(new SKR_whiteDwarfLoot());

        targetPlagueC.getStarSystem().getTags().add(Tags.THEME_UNSAFE);
        targetPlagueC.getStarSystem().getTags().add(SKR_ids.THEME_PLAGUEBEARER);
    }

    ////////////////////
    //                //
    //   CATACLYSM    //
    //                //
    ////////////////////

    private static void spawnCataclysm() {

        List<String> themesPlagueD = new ArrayList<>();
        themesPlagueD.add(Tags.THEME_INTERESTING);
        themesPlagueD.add(Tags.THEME_RUINS_MAIN);

        List<String> notThemesPlagueD = new ArrayList<>();
        notThemesPlagueD.add(SKR_ids.THEME_PLAGUEBEARER);
        notThemesPlagueD.add(Tags.THEME_HIDDEN);
        notThemesPlagueD.add("theme_already_occupied");
        notThemesPlagueD.add("theme_already_colonized");
        notThemesPlagueD.add("no_pulsar_blackhole");

        List<String> entitiesPlagueD = new ArrayList<>();
        entitiesPlagueD.add(Tags.GATE);

        SectorEntityToken targetPlagueD = MagicCampaign.findSuitableTarget(
                null,
                null,
                "FAR",
                themesPlagueD,
                notThemesPlagueD,
                entitiesPlagueD,
                true,
                true,
                true
        );

        if (targetPlagueD == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR CATACLYSM");
            return;
        } else {
            LOG.info("Adding CATACLYSM boss fleet in " + targetPlagueD.getStarSystem().getName());
        }

        Map<String, Integer> retinue = new HashMap<>();
        retinue.put("SKR_believer_assault", 1);
        retinue.put("SKR_believer_standard", 1);
        retinue.put("SKR_believer_support", 1);

        retinue.put("SKR_follower_assault", 1);
        retinue.put("SKR_follower_standard", 1);
        retinue.put("SKR_follower_support", 1);

        retinue.put("SKR_devotee_assault", 1);
        retinue.put("SKR_devotee_standard", 1);
        retinue.put("SKR_devotee_support", 1);

        retinue.put("SKR_doctrinaire_assault", 1);
        retinue.put("SKR_doctrinaire_standard", 1);
        retinue.put("SKR_doctrinaire_support", 1);

        retinue.put("SKR_fanatic_assault", 1);
        retinue.put("SKR_fanatic_standard", 1);
        retinue.put("SKR_fanatic_support", 1);

        retinue.put("SKR_cultist_assault", 1);
        retinue.put("SKR_cultist_standard", 1);
        retinue.put("SKR_cultist_support", 1);

        retinue.put("SKR_guru_assault", 1);
        retinue.put("SKR_guru_standard", 1);
        retinue.put("SKR_guru_support", 1);

        retinue.put("SKR_zealot_assault", 1);
        retinue.put("SKR_zealot_standard", 1);
        retinue.put("SKR_zealot_support", 1);

        PersonAPI plagueD = MagicCampaign.createCaptainBuilder("plague")
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setFirstName(txt("plague_D_boss"))
                .setLastName(txt("plague_D_boss"))
                .setPortraitId("SKR_plagueD")
                .setGender(FullName.Gender.ANY)
                .setFactionId("plague")
                .setRankId(Ranks.SPACE_COMMANDER)
                .setPostId(Ranks.POST_FLEET_COMMANDER)
                .setPersonality(Personalities.AGGRESSIVE)
                .setLevel(14)
                .setEliteSkillsOverride(14)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE)
                .create();

        CampaignFleetAPI cataclysm = MagicCampaign.createFleetBuilder()
                .setFleetName(txt("plague_D_fleet"))
                .setFleetFaction("plague")
                .setFlagshipName(txt("plague_D_boss"))
                .setFlagshipVariant("SKR_cataclysm_1")
                .setCaptain(plagueD)
                .setSupportFleet(retinue)
                .setMinFP(0)
                .setQualityOverride(2f)
                .setAssignment(FleetAssignment.PATROL_SYSTEM)
                .setAssignmentTarget(targetPlagueD)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();
        cataclysm.setDiscoverable(true);
        cataclysm.addTag(Tags.NEUTRINO);
        cataclysm.addTag(Tags.NEUTRINO_HIGH);
        cataclysm.addTag("SKR_cataclysm");
        cataclysm.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
        cataclysm.getFlagship().getVariant().addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        cataclysm.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        Global.getSector().getMemoryWithoutUpdate().set("$SKR_cataclysm_boss", true);
        cataclysm.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        cataclysm.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);

        cataclysm.addEventListener(new SKR_cataclysmLoot());

        Map<String, Integer> flagships = new HashMap<>();
        flagships.put("SKR_cultist_assault", 3);
        flagships.put("SKR_doctrinaire_assault", 4);
        flagships.put("SKR_fanatic_assault", 4);
        flagships.put("SKR_zealot_assault", 6);
        flagships.put("SKR_zealot_standard", 6);
        flagships.put("SKR_guru_standard", 6);
        flagships.put("SKR_guru_assault", 6);

        List<String> flagshipVariants = new ArrayList<>(flagships.keySet());

        //add roaming fleets        
        for (int i = 0; i < 15; i++) {
            String flagshipVariant = flagshipVariants.get(MathUtils.getRandomNumberInRange(0, flagships.size() - 1));
            int size = flagships.get(flagshipVariant);

            PersonAPI cultist = MagicCampaign.createCaptainBuilder("plague")
                    .setFirstName(txt("plague_D_boss"))
                    .setLastName(txt("plague_cultist"))
                    .setPortraitId("SKR_cultist")
                    .setGender(FullName.Gender.ANY)
                    .setFactionId("plague")
                    .setRankId(Ranks.SPACE_CAPTAIN)
                    .setPostId(Ranks.POST_PATROL_COMMANDER)
                    .setPersonality(Personalities.AGGRESSIVE)
                    .setLevel(4 + MathUtils.getRandomNumberInRange(2, size))
                    .setEliteSkillsOverride(3 + MathUtils.getRandomNumberInRange(1, size))
                    .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE)
                    .create();

            List<FleetAssignment> assignment = new ArrayList<>();
            {
                assignment.add(FleetAssignment.PATROL_SYSTEM);
                assignment.add(FleetAssignment.FOLLOW);
                assignment.add(FleetAssignment.ORBIT_AGGRESSIVE);
            }
            CampaignFleetAPI posse = MagicCampaign.createFleetBuilder()
                    .setFleetName(txt("plague_cultistFleet"))
                    .setFleetFaction("plague")
                    .setFleetType(FleetTypes.PATROL_MEDIUM)
                    .setFlagshipName(txt("plague_blank"))
                    .setFlagshipVariant(flagshipVariant)
                    .setCaptain(cultist)
                    .setMinFP(size * 40)
                    .setReinforcementFaction("plague")
                    .setQualityOverride(2f)
                    .setAssignment(assignment.get(MathUtils.getRandomNumberInRange(0, assignment.size() - 1)))
                    .setAssignmentTarget(cataclysm)
                    .create();
        }

        targetPlagueD.getStarSystem().getTags().add(Tags.THEME_UNSAFE);
        targetPlagueD.getStarSystem().getTags().add(SKR_ids.THEME_PLAGUEBEARER);
    }

    ////////////////////
    //                //
    //    DEMETER     //
    //                //
    ////////////////////

    private static void spawnDemeter() {
        List<String> marketDemeter = new ArrayList<>();
        marketDemeter.add("eochu_bres");

        List<String> factionDemeter = new ArrayList<>();
        factionDemeter.add("tritachyon");

        SectorEntityToken targetDemeter = MagicCampaign.findSuitableTarget(
                marketDemeter,
                factionDemeter,
                "CORE",
                null,
                null,
                null,
                false,
                false,
                false
        );

        if (targetDemeter == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR DEMETER");
            return;
        } else {
            LOG.info("Adding DEMETER fleet in " + targetDemeter.getStarSystem().getName());
        }

        PersonAPI demeterCaptain = MagicCampaign.createCaptainBuilder("tritachyon")
                .setFirstName("demeterCaptainFN")
                .setLastName("demeterCaptainLN")
                .setPortraitId("SKR_demeter")
                .setGender(FullName.Gender.MALE)
                .setFactionId("tritachyon")
                .setRankId(Ranks.SPACE_CHIEF)
                .setPostId(Ranks.POST_ENTREPRENEUR)
                .setPersonality(Personalities.CAUTIOUS)
                .setLevel(4)
                .setEliteSkillsOverride(-1)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_NO_MISSILE_YES_DEFENSE)
                .create();

        CampaignFleetAPI demeter = MagicCampaign.createFleetBuilder()
                .setFleetName("demeterFleet")
                .setFleetFaction("independent")
                .setFleetType(FleetTypes.FOOD_RELIEF_FLEET)
                .setFlagshipName("demeterShip")
                .setFlagshipVariant("CIV_demeter_standard")
                .setFlagshipAlwaysRecoverable(true)
                .setFlagshipAutofit(false)
                .setCaptain(demeterCaptain)
                .setMinFP(100)
                .setReinforcementFaction("tritachyon")
                .setQualityOverride(2f)
                .setAssignment(FleetAssignment.ORBIT_PASSIVE)
                .setAssignmentTarget(targetDemeter)
                .setIsImportant(false)
                .setTransponderOn(true)
                .create();
        demeter.setDiscoverable(false);
        demeter.addTag(Tags.NEUTRINO);
        demeter.getFlagship().getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        Global.getSector().getMemoryWithoutUpdate().set("$SKR_demeter", false);
    }

    ////////////////////
    //                //
    //    TITANIC     //
    //                //
    ////////////////////

    private static void spawnTitanic() {

        List<String> themesTitanic = new ArrayList<>();
        themesTitanic.add("procgen_no_theme");
        themesTitanic.add(Tags.THEME_UNSAFE);

        List<String> entitiesTitanic = new ArrayList<>();
        entitiesTitanic.add(Tags.ACCRETION_DISK);

        SectorEntityToken targetTitanic = MagicCampaign.findSuitableTarget(
                null,
                null,
                "CLOSE",
                themesTitanic,
                null,
                entitiesTitanic,
                true,
                true,
                true
        );

        if (targetTitanic == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR TITANIC");
            return;
        } else {
            LOG.info("Adding TITANIC wreck in " + targetTitanic.getStarSystem().getName());
        }

        SectorEntityToken titanicWreck = MagicCampaign.createDerelict(
                "CIV_titanic_standard",
                ShipRecoverySpecial.ShipCondition.WRECKED,
                true,
                1000,
                true,
                targetTitanic.getStarSystem().getStar(),
                0,
                targetTitanic.getStarSystem().getStar().getRadius() * 4,
                180
        );

        titanicWreck.setName(txt("titanicWreck"));
        titanicWreck.addTag(Tags.NEUTRINO);

        MagicCampaign.addSalvage(titanicWreck.getCargo(),
                titanicWreck,
                MagicCampaign.lootType.COMMODITY,
                Commodities.LUXURY_GOODS,
                120
        );

        MagicCampaign.addSalvage(titanicWreck.getCargo(),
                titanicWreck,
                MagicCampaign.lootType.COMMODITY,
                Commodities.LOBSTER,
                92
        );
    }

    ////////////////////
    //                //
    //     VOULGE     //
    //                //
    ////////////////////

    private static void spawnVoulge() {

        List<String> themesVoulge = new ArrayList<>();
        themesVoulge.add("procgen_no_theme");
        themesVoulge.add(Tags.THEME_UNSAFE);
        themesVoulge.add(Tags.THEME_INTERESTING_MINOR);

        List<String> entitiesVoulge = new ArrayList<>();
        entitiesVoulge.add(Tags.DEBRIS_FIELD);
        entitiesVoulge.add(Tags.GATE);
        entitiesVoulge.add(Tags.SALVAGEABLE);

        SectorEntityToken targetVoulge = MagicCampaign.findSuitableTarget(
                null,
                null,
                "CLOSE",
                themesVoulge,
                null,
                entitiesVoulge,
                true,
                true,
                true
        );

        if (targetVoulge == null) {
            LOG.info("NO SYSTEM AVAILABLE FOR VOULGE");
            return;
        } else {
            LOG.info("Adding VOULGE wreck in " + targetVoulge.getStarSystem().getName());
        }

        SectorEntityToken voulgeWreck = MagicCampaign.createDerelict(
                "SKR_voulge_outdated",
                ShipRecoverySpecial.ShipCondition.BATTERED,
                true,
                500,
                true,
                targetVoulge,
                0,
                200,
                180
        );

        voulgeWreck.setName(txt("voulgeWreck"));
        voulgeWreck.addTag(Tags.NEUTRINO);
    }
}