use dashmap::DashMap;
use once_cell::sync::Lazy;

use crate::entities::prelude::MobTemplate;

static MOB_TEMPLATES: Lazy<DashMap<i16, MobTemplate>> = Lazy::new(|| DashMap::new());
