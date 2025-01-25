package red.man10.man10structureapartment

import org.bukkit.Material
import org.bukkit.entity.Player

class MainMenu(p: Player) : MenuFramework(p, 9, "§b§l[CloudApartment]") {

    override fun init() {

        val fill = Button(Material.CYAN_STAINED_GLASS_PANE)
        fill.setClickAction{ it.isCancelled = true }

        fill(fill)

        val rentButton = Button(Material.GOLD_INGOT)
        rentButton.cmd(1)
        rentButton.title("§e§l利用料を支払う(${StructureManager.dailyRent*30}円)")
        rentButton.lore(mutableListOf("§f30日分のマンション利用料を支払います"
            ,"§f30日を超えるとマンションの利用ができなくなります"))
        rentButton.setClickAction{
            p.performCommand("msa pay 30")
        }

        setButton(rentButton,2)

        val jumpButton = Button(Material.OAK_DOOR)
        jumpButton.title("§a§lマンションにテレポートする")
        jumpButton.lore(mutableListOf("§fマンションにテレポートします","§e§l初めて入居する場合は左のボタンから利用料を払ってください"))
        jumpButton.setClickAction{
            p.performCommand("msa jump")
        }

        setButton(jumpButton,6)
    }

}