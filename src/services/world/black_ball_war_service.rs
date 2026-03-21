use crate::network::session::SessionArc;
use crate::player::Player;

pub struct BlackBallWarService;

impl BlackBallWarService {
    pub fn change_map(
        _player: &mut Player,
        _index: i8,
        _session: &SessionArc,
    ) -> anyhow::Result<()> {
        Ok(())
    }
}
