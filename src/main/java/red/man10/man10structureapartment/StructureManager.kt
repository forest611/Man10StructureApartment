package red.man10.man10structureapartment

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import red.man10.man10structureapartment.Man10StructureApartment.Companion.instance
import java.io.File
import java.util.*

object StructureManager {

    fun save(pos1:Location,pos2:Location,apartName:String,owner:UUID){

        val manager = instance.server.structureManager
        val structure = manager.createStructure()

        structure.fill(pos1,pos2,true)

        val file = File("${instance.dataFolder.path}/${apartName}/${owner}")

        try {
            if (!file.exists()){
                manager.saveStructure(file,structure)
            }else{
                Bukkit.getLogger().info("上書きします")
                manager.saveStructure(file,structure)
            }
        }catch (e:Exception){
            Bukkit.getLogger().warning("書き込みエラー アパート名:${apartName} 持ち主:${owner}")
        }
    }

    fun place(owner: UUID,apartName: String,pos:Location){
        val manager = instance.server.structureManager
        val file = File("${instance.dataFolder.path}/${apartName}/${owner}")
        val structure = manager.loadStructure(file)

        structure.place(pos,true,StructureRotation.NONE,Mirror.NONE,0,1F, Random())
        
    }


}