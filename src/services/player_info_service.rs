use crate::data::DataGame;
use crate::network::async_net::session::AsyncSession;
use crate::network::async_net::message::Message;
use crate::services::IntrinsicService;
use crate::player::Player as RtPlayer;
use crate::models::zone::ZONE_MANAGER;

pub struct PlayerInfoService;

impl PlayerInfoService {
  
    pub async fn send_point_info(session: &mut AsyncSession, player: &RtPlayer) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending point info");

        let n = &player.n_point;
        let hp_max = n.hp_max as i64;
        let mp_max = n.mp_max as i64;
        let hp = n.hp as i64;
        let mp = n.mp as i64;
        let damage = n.damage as i64;
        let defense = n.defense as i64;
        let crit = (n.crit.min(255)) as u8;
        let power = n.power as i64;
        let speed: u8 = 10; 

        let mut msg = Message::new_for_writing(-42);
        msg.write_long(hp_max)?; // hpg
        msg.write_long(mp_max)?; // mpg
        msg.write_long(damage)?; // dameg
        msg.write_long(hp_max)?; // hpMax
        msg.write_long(mp_max)?; // mpMax
        msg.write_long(hp)?;     // hp
        msg.write_long(mp)?;     // mp
        msg.write_byte(speed as i8)?;  // speed
        msg.write_byte(20)?;     // reserved
        msg.write_byte(20)?;     // reserved
        msg.write_byte(1)?;      // reserved
        msg.write_long(damage)?; // dame
        msg.write_long(defense)?;// def
        msg.write_byte(crit as i8)?;   // crit
        msg.write_long(power)?;  // tiemNang
        msg.write_short(0)?;     // reserved
        msg.write_long(0)?;      // defg (reserved)
        msg.write_byte(0)?;      // critg (reserved)
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_task_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending task main info");
        
        let mut msg = Message::new_for_writing(40);
        msg.write_short(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // taskMain.id
        msg.write_byte(0).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // taskMain.index
        msg.write_utf("Nhiệm vụ chính[1]").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // taskMain.name + "[" + taskMain.id + "]"
        msg.write_utf("Chi tiết nhiệm vụ chính").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // taskMain.detail
        msg.write_byte(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // subTasks.size()
        
        // Sub task info
        msg.write_utf("Nhiệm vụ phụ").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // stm.name
        msg.write_byte(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // stm.npcId
        msg.write_short(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // stm.mapId
        msg.write_utf("Thông báo nhiệm vụ").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // stm.notify
        
        msg.write_short(0).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // current subTask count
        msg.write_short(10).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // stm.maxCount
        
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Clear map (-22)
    pub async fn clear_map(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Clearing map");
        
        let mut msg = Message::new_for_writing(-22);
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Send clan info (-53) - matches Java ClanService.sendMyClan()
    pub async fn send_clan_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending clan info");
        
        let mut msg = Message::new_for_writing(-53);
        // For now, send -1 to indicate no clan
        msg.write_int(-1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // clan.id or -1 if no clan
        
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Send max stamina (-69)
    pub async fn send_max_stamina(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending max stamina");
        
        let mut msg = Message::new_for_writing(-69);
        msg.write_int(100).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // max stamina
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Send current stamina (-68)
    pub async fn send_current_stamina(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending current stamina");
        
        let mut msg = Message::new_for_writing(-68);
        msg.write_int(100).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // current stamina
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }



    /// Send pet info (-107) - matches Java Service.sendHavePet()
    pub async fn send_pet_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending pet info");
        let mut msg = Message::new_for_writing(-107);
        msg.write_byte(0).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // has pet (0 = no pet, 1 = has pet)
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Send top rank info (-119)
    pub async fn send_top_rank_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending top rank info");
        
        let mut msg = Message::new_for_writing(-119);
        msg.write_utf("1630679754740_-119_r").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Send notification tab (-50)
    pub async fn send_notification_tab(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-50);
        msg.write_byte(0).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // notification count
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }
    /// Send big message (-70)
    pub async fn send_big_message(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-70);
        msg.write_utf("Chào mừng đến với GameNro!").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    pub async fn send_time_skill(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending time skill info");
        
        let mut msg = Message::new_for_writing(-30);
        msg.write_byte(62).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // sub command
        // TODO: Implement skill time data
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        Ok(())
    }

    /// Clear VTSK (clear skill data)
    pub async fn clear_vtsk(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
    
        Ok(())
    }

    pub async fn send_all_player_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending all player info");
        let player = session.get_player().cloned().ok_or("Player not set")?;
        DataGame::send_tile_set_info(session).await?;
        
        // 112 my info intrinsic
        let intrinsic_service: IntrinsicService = IntrinsicService;
        intrinsic_service.send_info_intrinsic(session, &player).await?;
        
        // -42 my point
        Self::send_point_info(session, &player).await?;
        
        // 40 task
        Self::send_task_info(session).await?;
        
        // -22 reset all
        Self::clear_map(session).await?;
        
        // -53 my clan
        Self::send_clan_info(session).await?;
        
        // -69 max stamina
        Self::send_max_stamina(session).await?;
        
        // -68 cur stamina
        Self::send_current_stamina(session).await?;
        
        // -107 have pet
        Self::send_pet_info(session).await?;
        
        // -119 top rank
        Self::send_top_rank_info(session).await?;
        
        // -50 thông tin bảng thông báo
        Self::send_notification_tab(session).await?;
        
        {
            let zone = {
                let mut mgr = ZONE_MANAGER.write().await;
                if let Some(z) = mgr.get_zone(player.map_id as i32, player.zone_id as i32).await {
                    z
                } else {
                    mgr.create_zone(player.map_id as i32, player.zone_id as i32, 50).await?;
                    mgr.get_zone(player.map_id as i32, player.zone_id as i32).await.unwrap()
                }
            };
            zone.add_player(player.clone()).await?;
            // Send my info to others and others' info to me
            zone.load_me_to_another(player.id).await?;
            zone.load_another_to_me(player.id).await?;
            zone.map_info(session, player.id).await?;
        }
        // -70 thông báo bigmessage
        Self::send_big_message(session).await?;
        
        // last time use skill
        Self::send_time_skill(session).await?;
        
        // clear vt sk
        Self::clear_vtsk(session).await?;
        
        println!("All player info sent successfully");
        Ok(())
    }
}
