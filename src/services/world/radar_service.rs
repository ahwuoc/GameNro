use crate::constant::cmd::cmd;
use crate::models::radar::{Card, RadarCardTemplate};
use crate::network::message::Message;
use crate::player::Player;
use crate::templates::radar_template_manager;
use anyhow::Result;

pub struct RadarService;

impl RadarService {
    pub fn send_radar(player: &Player, player_cards: &[Card]) -> Result<()> {
        let mut msg = Message::new(cmd::RADAR);
        msg.write_byte(0)?;

        let templates = radar_template_manager::get_all();
        msg.write_short(templates.len() as i16)?;

        for radar in templates {
            let card_opt = player_cards.iter().find(|c| c.id == radar.id);

            msg.write_short(radar.id)?;
            msg.write_short(radar.icon_id)?;
            msg.write_byte(radar.rank)?;

            if let Some(card) = card_opt {
                msg.write_byte(card.amount)?;
                msg.write_byte(card.max_amount)?;
            } else {
                msg.write_byte(0)?; // current amount
                msg.write_byte(radar.max)?; // max from template
            }

            msg.write_byte(radar.type_radar)?;
            match radar.type_radar {
                0 => {
                    msg.write_short(radar.template)?; // Mob template
                }
                1 => {
                    msg.write_short(radar.head)?;
                    msg.write_short(radar.body)?;
                    msg.write_short(radar.leg)?;
                    msg.write_short(radar.bag)?;
                }
                _ => {
                    msg.write_short(-1)?;
                }
            }

            msg.write_utf(&radar.name)?;
            msg.write_utf(&radar.info)?;

            if let Some(card) = card_opt {
                msg.write_byte(card.level)?;
                msg.write_byte(card.used)?;
            } else {
                msg.write_byte(0)?; // Level
                msg.write_byte(0)?; // Used
            }

            // Options
            msg.write_byte(radar.options.len() as i8)?;
            for opt in &radar.options {
                msg.write_byte(opt.id as i8)?;
                msg.write_short(opt.param as i16)?;
                msg.write_byte(opt.active_card)?;
            }
        }
        player.send_to_client(msg)?;
        Ok(())
    }

    pub fn send_radar_1(player: &Player, card_id: i16, used: i8) -> Result<()> {
        let mut msg = Message::new(cmd::RADAR);
        msg.write_byte(1)?; // actionRadar = 1 (Radar1)
        msg.write_short(card_id)?;
        msg.write_byte(used)?;
        player.send_to_client(msg)?;
        Ok(())
    }
}
