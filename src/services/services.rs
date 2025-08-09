use tokio::task::JoinHandle;
use tokio::time::{interval, Duration, MissedTickBehavior};

pub struct ServiceHandles {
    pub update: JoinHandle<()>,
    pub db_maintenance: JoinHandle<()>,
    pub boss_manager: JoinHandle<()>,
    pub clan: JoinHandle<()>,
    pub shop: JoinHandle<()>,
    pub event: JoinHandle<()>,
}

pub fn spawn_all_services() -> ServiceHandles {
    let update = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_millis(500));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] update started");
        loop {
            ticker.tick().await;
            // TODO: world updates
        }
    });

    let db_maintenance = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_secs(300));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] db maintenance started");
        loop {
            ticker.tick().await;
            // TODO: cleanup sessions, save stats, clan data, events
        }
    });

    let boss_manager = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_secs(60));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] boss manager started");
        loop {
            ticker.tick().await;
            // TODO: spawn/update bosses
        }
    });

    let clan = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_secs(600));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] clan started");
        loop {
            ticker.tick().await;
            // TODO: clan stats/wars
        }
    });

    let shop = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_secs(300));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] shop started");
        loop {
            ticker.tick().await;
            // TODO: shop updates/auctions/consignments
        }
    });

    let event = tokio::spawn(async move {
        let mut ticker = interval(Duration::from_secs(60));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        println!("[service] event started");
        loop {
            ticker.tick().await;
            // TODO: scheduled events
        }
    });

    ServiceHandles { update, db_maintenance, boss_manager, clan, shop, event }
}


