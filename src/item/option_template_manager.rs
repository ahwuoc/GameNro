
use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};

use crate::entities::item_option_template::{self, Model as ItemOptionModel};


static  ITEM_OPTION_TEMPLATES:Lazy<DashMap<i8,ItemOptionModel>> = Lazy::new(||DashMap::new());


pub async  fn load(db:&DatabaseConnection)->anyhow::Result<()>{
     let rows =     item_option_template::Entity::find().all(db).await?;
      for row in rows{
          ITEM_OPTION_TEMPLATES.insert(row.id as i8, row);
      }
     Ok(())
}
pub fn get_all()->Vec<ItemOptionModel>{
     ITEM_OPTION_TEMPLATES.iter().map(|kv|kv.value().clone()).collect()
}
pub fn get(id:i8)->Option<ItemOptionModel>{
    ITEM_OPTION_TEMPLATES.get(&id).map(|v|v.clone())
}