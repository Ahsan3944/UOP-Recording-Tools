package com.uop.recordingtools.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.io.IOException;

public final class DataStore {
    private final JavaPlugin plugin; private final File dir; private YamlConfiguration data;
    public DataStore(JavaPlugin plugin){this.plugin=plugin; this.dir=new File(plugin.getDataFolder(),"data");}
    public synchronized void load(){ dir.mkdirs(); File f=new File(dir,"data.yml"); data=YamlConfiguration.loadConfiguration(f); }
    public synchronized void save(){ if(data==null)return; try{data.save(new File(dir,"data.yml"));}catch(IOException e){plugin.getLogger().severe("Could not save data: "+e.getMessage());} }
    public synchronized YamlConfiguration data(){return data;}
    public synchronized void set(String path,Object value){data.set(path,value);}
    public synchronized Object get(String path){return data.get(path);}
    public synchronized void remove(String path){data.set(path,null);}
    public synchronized void saveNow(){save();}
}
