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
import red.man10.man10structureapartment.Man10StructureApartment.Companion.msg
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

    private var distance = 64
    private var maxApartCount = 128
    var dailyRent = 1000.0
    private lateinit var world: World
    private var backCommand = ""

    private var addressMap = ConcurrentHashMap<UUID,ApartData>()
    private val livingList = mutableListOf<UUID>()
    private lateinit var manager : StructureManager
    private lateinit var defaultBuilding : Structure
    private lateinit var vault : VaultManager
    private lateinit var jump : Triple<Double,Double,Double>

    fun load(){
        manager = instance.server.structureManager
        vault = VaultManager(instance)

        instance.saveDefaultConfig()
        instance.reloadConfig()

        distance = instance.config.getInt("Distance")
        maxApartCount = instance.config.getInt("MaxApartCount")
        dailyRent = instance.config.getDouble("DailyRent")
        world = instance.server.getWorld(instance.config.getString("BuilderWorld")?:"world")!!
        backCommand = instance.config.getString("BackCommand")?:""

        val relativeX = instance.config.getDouble("RelativeX")
        val relativeY = instance.config.getDouble("RelativeY")
        val relativeZ = instance.config.getDouble("RelativeZ")

        jump = Triple(relativeX,relativeY,relativeZ)

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
        Bukkit.getLogger().info("初期建築を読み込みました")
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

        array.forEach { addressMap[it.owner] = it }

        reader.close()

        Bukkit.getLogger().info("住所情報を読み込みました")
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
    fun saveStructure(uuid:UUID){

        val data = addressMap[uuid]?:return

        val structure = manager.createStructure()

        val pos1 = strToLoc(data.pos1)
        val pos2 = strToLoc(data.pos2)

        structure.fill(pos1,pos2,true)
        structure.persistentDataContainer.set(NamespacedKey(instance,"RentDue"), PersistentDataType.LONG,data.rentDue.time)

        val file = File("${instance.dataFolder.path}/Apart/${uuid}")

        try {
            if (!file.exists()){
                manager.saveStructure(file,structure)
            }else{
                manager.saveStructure(file,structure)
            }
            file.copyTo(File("${instance.dataFolder.path}/Apart/backup/${uuid}/${SimpleDateFormat("MM_dd_HH_mm_ss").format(
                Date()
            )}"))
        }catch (e:Exception){
            e.printStackTrace()
            val p = Bukkit.getPlayer(uuid)?:return
            msg(p,"§cアパートの保存に失敗しました。レポートしてください")
        }
    }

    //  ストラクチャーの呼び出し
    //  ストラクチャーが生成できたらtrueを返す
    fun placeStructure(p:Player):Boolean{

        //アドレスがすでにある場合は建物があるとしてリターン
        if (addressMap[p.uniqueId]!=null){
            return true
        }

        //座標を仮設定(現在あるアパートの数から指定する
        var pos1 = Location(world,(addressMap.size * distance).toDouble(),100.0,0.0)

        //最大数に達していたら、一番古いアパートと置き換える(そこの住人がオンラインだった場合は諦める)
        if (addressMap.size >= maxApartCount){
            val oldestData = addressMap.values.filter { !Bukkit.getOfflinePlayer(it.owner).isOnline }.minByOrNull { it.lastAccess }

            if (oldestData == null){
                msg(p,"§c現在マンションは定員オーバーです")
                return false
            }

            saveStructure(oldestData.owner)
            removeStructure(oldestData.owner)
            pos1 = strToLoc(oldestData.pos1)
        }

        val file = File("${instance.dataFolder.path}/Apart/${p.uniqueId}")

        val structure = if (file.exists()){
            manager.loadStructure(file)
        } else {
            val default = File("${instance.dataFolder.path}/Apart/Default")
            if (!default.exists()){
                msg(p,"§cアパートの初期値がありません。レポートしてください")
                return false
            }
            manager.loadStructure(default)
        }

        file.copyTo(File("${instance.dataFolder.path}/Apart/backup/${p.uniqueId}/${SimpleDateFormat("MM_dd_HH_mm_ss").format(Date())}"))

        //建物を設置
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

//        msg(p,"設置完了")
        return true
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
            //ブロックを削除
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        world.getBlockAt(x, y, z).type = Material.AIR
                    }
                }
            }

            //エンティティを削除
            for (e in world.entities){
                val loc = e.location
                if (loc.blockX in minX..maxX && loc.blockY in minY..maxY && loc.blockZ in minZ .. maxZ){
                    e.remove()
                }
            }
        })

        addressMap.remove(owner)
        saveAddress()
    }

    fun addPayment(p:Player,day:Int){

        val data = addressMap[p.uniqueId]

        if (data==null){
            val ret = placeStructure(p)

            if (ret){addPayment(p,day)}
            return
        }

        if (!vault.withdraw(p.uniqueId,day* dailyRent)){
            msg(p,"§c電子マネーが足りません")
            return
        }

        val cal = Calendar.getInstance()
        cal.time = data.rentDue
        cal.add(Calendar.DAY_OF_YEAR,day)
        data.rentDue = cal.time

        addressMap[p.uniqueId] = data
        saveAddress()
        saveStructure(p.uniqueId)

        msg(p,"§e§l利用料の支払いを行いました(利用可能期間:${SimpleDateFormat("MM月dd日").format(data.rentDue)}まで)")
    }

    fun jump(p:Player){

        //マンションにいたら戻る
        if (livingList.contains(p.uniqueId)){
            exit(p)
            p.performCommand(backCommand)
            return
        }

        val data = addressMap[p.uniqueId]

        if (data == null){
            val ret = placeStructure(p)
            if (ret){
                jump(p)
            }else{
                msg(p,"§cマンションの呼び出しに失敗しました。レポートしてください")
            }
            return
        }

        if (Date().after(data.rentDue)){
            msg(p,"§c§l利用料の支払いがされていません！")
            return
        }

        val pos1 = strToLoc(data.pos1)

        val spawnX = pos1.x+ jump.first
        val spawnY = pos1.y+ jump.second
        val spawnZ = pos1.z+ jump.third

        val loc = Location(pos1.world,spawnX,spawnY,spawnZ)

        p.teleport(loc)
        enter(p)
        msg(p,"§a§lマンションにジャンプしました")
        msg(p,"§a§l戻る時は§n§lドアを右クリック§a§lしてください")
    }

    fun enter(p:Player){
        livingList.add(p.uniqueId)
    }

    fun exit(p:Player){
        livingList.remove(p.uniqueId)
    }
}

data class ApartData(
    val owner : UUID,
    val pos1 : String,
    val pos2 : String,
    val lastAccess : Date,
    var rentDue : Date
)