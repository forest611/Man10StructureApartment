package red.man10.man10structureapartment

import com.google.gson.Gson
import org.bukkit.*
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.structure.Structure
import org.bukkit.structure.StructureManager
import red.man10.man10structureapartment.Man10StructureApartment.Companion.instance
import red.man10.man10structureapartment.Man10StructureApartment.Companion.locToStr
import red.man10.man10structureapartment.Man10StructureApartment.Companion.strToLoc
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min


object StructureManager {

    var distance = 64
    var maxApartCount = 128
    var dailyRent = 1000.0
    lateinit var world: World

    private var addressMap = ConcurrentHashMap<UUID,ApartData>()
    private lateinit var manager : StructureManager
    private lateinit var defaultBuilding : Structure
    private lateinit var vault : VaultManager

    fun load(){
        manager = instance.server.structureManager
        vault = VaultManager(instance)
        loadDefault()
        loadAddress()
    }

    //初期建築の読み込み
    private fun loadDefault(){
        val default = File("${instance.dataFolder.path}/Apart/Default")
        if (!default.exists()){
            Bukkit.getLogger().warning("初期建築がありません")
            return
        }
        defaultBuilding = manager.loadStructure(default)

    }

    fun saveDefault(p:Player, pos1:Location, pos2:Location){
        val structure = manager.createStructure()
        structure.fill(pos1,pos2,true)

        val file = File("${instance.dataFolder.path}/Apart/Default")

        try {
            manager.saveStructure(file,structure)
            defaultBuilding = structure
            p.sendMessage("保存成功")
        }catch (e:Exception){
            p.sendMessage("§c初期建築の保存に失敗")
        }
    }

    private fun loadAddress(){

        addressMap.clear()

        val file = File("${instance.dataFolder.path}/Address.json")

        if (!file.exists()){
            Bukkit.getLogger().warning("住所情報がありません")
            return
        }

        val reader = FileReader(file)

        val array = Gson().fromJson(reader.readText(),Array<ApartData>::class.java)

        array.forEach {
            addressMap[it.owner] = it
        }

        reader.close()
    }

    private fun saveAddress(){

        val jsonStr = Gson().toJson(addressMap.values.toTypedArray())

        val file = File("${instance.dataFolder.path}/Address.json")

        val writer = FileWriter(file)

        writer.write(jsonStr)

        writer.close()
    }

    private fun update(data: ApartData){
        addressMap[data.owner] = data
        saveAddress()
    }

    //  ストラクチャーの保存
    fun saveStructure(p:Player){

        val data = addressMap[p.uniqueId]?:return

        val structure = manager.createStructure()

        val pos1 = strToLoc(data.pos1)
        val pos2 = strToLoc(data.pos2)

        structure.fill(pos1,pos2,true)
        structure.persistentDataContainer.set(NamespacedKey(instance,"RentDue"), PersistentDataType.LONG,data.rentDue.time)

        val file = File("${instance.dataFolder.path}/Apart/${p.uniqueId}")

        try {
            if (!file.exists()){
                manager.saveStructure(file,structure)
            }else{
                Bukkit.getLogger().info("上書きします")
                manager.saveStructure(file,structure)
            }
            p.sendMessage("保存完了")
        }catch (e:Exception){
            p.sendMessage("§cアパートの保存に失敗しました。レポートしてください")
        }

    }

    //  ストラクチャーの呼び出し
    fun placeStructure(p:Player){

        if (addressMap[p.uniqueId]!=null){
            p.sendMessage("すでに建物があります")
            return
        }

        //座標を仮設定(現在あるアパートの数から指定する
        var pos1 = Location(world,(addressMap.size * distance).toDouble(),100.0,0.0)

        //最大数に達していたら、一番古いアパートと置き換える(そこの住人がオンラインだった場合は諦める)
        if (addressMap.size >= maxApartCount){
            val oldestData = addressMap.values.filter { Bukkit.getPlayer(it.owner)?.isOnline == false }.minByOrNull { it.lastAccess }

            if (oldestData == null){
                p.sendMessage("現在マンションは定員オーバーです")
                return
            }

            removeStructure(oldestData.owner)
            pos1 = strToLoc(oldestData.pos1)
        }

        val file = File("${instance.dataFolder.path}/Apart/${p.uniqueId}")

        val structure = if (file.exists()){
            manager.loadStructure(file)
        } else {
            val default = File("${instance.dataFolder.path}/Apart/Default")
            if (!default.exists()){
                p.sendMessage("§cアパートの初期値がありません。レポートしてください")
                return
            }
            manager.loadStructure(default)
        }

        Bukkit.getScheduler().runTask(instance, Runnable {
            structure.place(pos1,true,StructureRotation.NONE,Mirror.NONE,-1,1F, Random())
        })


        val pos2 = pos1.clone()

        pos2.x+=structure.size.x
        pos2.y+=structure.size.y
        pos2.z+=structure.size.z

        val date = Date()
        date.time = structure.persistentDataContainer[NamespacedKey(instance,"RentDue"), PersistentDataType.LONG]?:Date().time

        //住所情報をJsonファイルに登録
        update(ApartData(p.uniqueId, locToStr(pos1), locToStr(pos2), Date(),date))

        p.sendMessage("設置完了")
    }

    //土地を削除する
    fun removeStructure(owner: UUID){

        val data = addressMap[owner]?:return

        val pos1 = strToLoc(data.pos1)
        val pos2 = strToLoc(data.pos2)

        val world = pos1.world

        val minX = min(pos1.blockX,pos2.blockX)
        val minY = min(pos1.blockY,pos2.blockY)
        val minZ = min(pos1.blockZ,pos2.blockZ)
        val maxX = max(pos1.blockX,pos2.blockX)
        val maxY = max(pos1.blockY,pos2.blockY)
        val maxZ = max(pos1.blockZ,pos2.blockZ)

        Bukkit.getScheduler().runTask(instance, Runnable {
            //エンティティを削除
            for (e in world.entities){
                val loc = e.location
                if (loc.blockX in minX..maxX && loc.blockY in minY..maxY && loc.blockZ in minZ .. maxZ){
                    e.remove()
                }
            }

            //ブロックを削除
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        world.getBlockAt(x, y, z).type = Material.AIR
                    }
                }
            }

        })

        addressMap.remove(owner)
        saveAddress()
    }

    fun addPayment(p:Player,day:Int){

        if (!vault.withdraw(p.uniqueId,day* dailyRent)){
            p.sendMessage("電子マネーが足りません")
            return
        }

        val data = addressMap[p.uniqueId]?:return
        val cal = Calendar.getInstance()
        cal.time = data.rentDue
        cal.add(Calendar.MINUTE,day)
        data.rentDue = cal.time

        addressMap[p.uniqueId] = data
        saveStructure(p)

        p.sendMessage("利用料の支払いを行いました(利用可能期間:${SimpleDateFormat("yyyy-MM-dd").format(data.rentDue)}まで)")
    }

    fun jump(p:Player){

        val data = addressMap[p.uniqueId]

        if (data == null){
//            p.sendMessage("あなたはマンションを借りていません")
            placeStructure(p)
            jump(p)
            return
        }

        if (Date().after(data.rentDue)){
            p.sendMessage("利用料の支払いがされていません！")
            return
        }

        val pos1 = strToLoc(data.pos1)
        val pos2 = strToLoc(data.pos2)

        val spawnX = (pos1.x + pos2.x)/2.0
        val spawnY = (pos1.y + pos2.y)/2.0
        val spawnZ = (pos1.z + pos2.z)/2.0

        val loc = Location(pos1.world,spawnX,spawnY,spawnZ)

        p.teleport(loc)
    }
}

data class ApartData(
    val owner : UUID,
    val pos1 : String,
    val pos2 : String,
    val lastAccess : Date,
    var rentDue : Date
)