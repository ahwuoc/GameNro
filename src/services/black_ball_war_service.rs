use crate::network::session::SessionArc;
use crate::player::Player;

pub struct BlackBallWarService;

impl BlackBallWarService {
    pub async fn change_map(
        _player: &mut Player,
        _index: i8,
        _session: &SessionArc,
    ) -> anyhow::Result<()> {
        // TODO: Implement black ball war map change
        Ok(())
    }
}
