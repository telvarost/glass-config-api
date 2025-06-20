package net.glasslauncher.mods.gcapi3test.impl.example;

import net.glasslauncher.mods.gcapi3.api.ConfigCategory;
import net.glasslauncher.mods.gcapi3.api.ConfigEntry;
import net.glasslauncher.mods.gcapi3.api.TriBoolean;
import net.glasslauncher.mods.gcapi3.api.ValueOnVanillaServer;

/**
 * An example config class, you can view this in-game inside modmenu's settings button for gcapi.
 */
public class ExampleConfigClass {

    @ConfigEntry(name = "Tested Config", // Shows up above the config entry in white, unless you use colour codes, then it will use those.
            description = "Used to translate nerd") // Shows up in grey under the config entry.
    public String testedConfig = "nerd";

    @ConfigEntry(name = "Tested Config 1",
        multiplayerSynced = true) // Marks this entry to be synced with the server on join, and when server config changes. Do not use for client-side configs, you will annoy your users.
    public String testConfig1 = "wow";

    @ConfigEntry(name = "ASD 2")
    public String asd2 = "hmmm";

    @ConfigEntry(name = "ASD 3", requiresRestart = true)
    public String asd3 = "hmmm";

    @ConfigEntry(name = "DoubleTest")
    public Double doubl = 12d;

    @ConfigEntry(name = "MP Synced Boolean", multiplayerSynced = true)
    @ValueOnVanillaServer(booleanValue = TriBoolean.TRUE)
    public Boolean mpBool = false;

    @ConfigEntry(name = "Test Enum", requiresRestart = true)
    public ExampleConfigEnum enumTest = ExampleConfigEnum.YAY;

    /**
     * A config category, you can put other categories inside a category too.
     * See the ExampleConfigCategory class for more details.
     */
    @ConfigCategory(name = "§6Oh Noes", description = "My config category")
    public ExampleConfigCategory configCategory = new ExampleConfigCategory();

}
