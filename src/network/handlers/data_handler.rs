use crate::data::data_game::DataGame;
use crate::data::ItemData;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::services::player_info_service;
use crate::templates::power_manager;
use anyhow::Result;
use tracing::{debug, error, info, warn};

pub struct DataHandler;

impl DataHandler {
    pub async fn get_image_source(session: &SessionArc, mut msg: Message) -> Result<()> {
        let type_byte = msg.read_byte()?;
        match type_byte {
            1 => DataGame::send_size_res(session).await?,
            2 => DataGame::send_res(session).await?,
            _ => warn!("Unknown type for -74 command: {}", type_byte),
        }
        Ok(())
    }

    pub async fn get_effect_template(session: &SessionArc, mut msg: Message) -> Result<()> {
        let eff_id = msg.read_short()?;
        let id_t = eff_id;
        DataGame::send_effect_template(session, eff_id, Some(id_t)).await?;
        Ok(())
    }

    pub async fn get_mob_template(session: &SessionArc, mut msg: Message) -> Result<()> {
        let mob_id = msg.read_byte()?;
        DataGame::send_mob_temp(session, mob_id).await?;
        Ok(())
    }

    pub async fn get_item_bg_template(session: &SessionArc, mut msg: Message) -> Result<()> {
        let bg_id = msg.read_short()?;
        DataGame::send_item_bg_template(session, bg_id).await?;
        Ok(())
    }

    pub async fn get_image_by_name(session: &SessionArc, mut msg: Message) -> Result<()> {
        let img_name = msg.read_utf()?;
        DataGame::send_image_by_name(session, &img_name).await?;
        Ok(())
    }

    pub async fn get_icon(session: &SessionArc, mut msg: Message) -> Result<()> {
        match msg.read_int() {
            Ok(id) => DataGame::send_icon(session, id).await?,
            Err(e) => error!("Error -67 {:?}", e),
        }
        Ok(())
    }

    pub fn get_captions(session: &SessionArc, mut msg: Message) -> Result<()> {
        match msg.read_byte() {
            Ok(gender) => {
                let captions = power_manager::get_all_captions();
                let mut response = Message::new(-41);
                response.write_byte(captions.len() as i8)?;

                let planet_name = match gender {
                    0 => "Trái Đất",
                    1 => "Namếc",
                    2 => "Xayda",
                    _ => "",
                };

                for caption in captions {
                    let name = caption.name.replace("{planet}", planet_name);
                    response.write_utf(&name)?;
                }
                session.transmit(response);
            }
            Err(e) => {
                error!("Error reading byte {}", e);
            }
        }
        Ok(())
    }

    pub async fn update_data(session: &SessionArc) -> Result<()> {
        if let Err(e) = DataGame::update_data(session).await {
            error!("Error updating data {}", e);
        }
        Ok(())
    }

    pub async fn send_key(session: &SessionArc) -> Result<()> {
        if let Err(e) = session.send_key_async().await {
            error!("Error sending key {}", e);
        }
        session.set_sent_key(true).await;
        if let Err(e) = DataGame::send_version_res(session).await {
            error!("Error sending version res {}", e);
        }
        Ok(())
    }

    pub async fn handle_not_map(session: &SessionArc, mut msg: Message) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        match sub_cmd {
            2 => {
                crate::network::handlers::auth_handler::AuthHandler::handle_create_char(
                    session, msg,
                )
                .await?;
            }
            6 => DataGame::update_map(session).await?,
            7 => DataGame::update_skill(session).await?,
            8 => ItemData::update_item(session).await?,
            10 => {
                let map_id = msg.read_byte()?;
                DataGame::send_map_temp(session, map_id as u8).await?;
            }
            13 => {
                let player_opt = session.get_player_snapshot().await;
                if let Some(player) = player_opt {
                    player_info_service::send_player_blob_internal(&player).await?;
                    info!(
                        "Client ok enhanced initialization completed for player: {}",
                        player.name
                    );
                } else {
                    debug!("Client ok enhanced: Player not set yet, ignoring");
                }
            }
            _ => warn!("Unknown -28 sub-command: {}", sub_cmd),
        }
        Ok(())
    }
}
