use crate::{combine::combine_type::CombineType, item::item::Item};

#[derive(Default, Debug, Clone)]
pub struct Combine {
    pub last_time_combine: i64,
    pub items_combine: Vec<Item>,
    pub type_combine: CombineType,
    pub gold_combine: i32,
    pub gem_combine: i32,
    pub ratio_combine: f32,
    pub count_da_nang_cap: i32,
    pub count_da_bao_ve: i16,
}

impl Combine {
    pub fn new() -> Self {
        Combine {
            items_combine: Vec::new(),
            ..Default::default()
        }
    }

    pub fn clear_item_combine(&mut self) {
        self.items_combine.clear();
    }

    pub fn clear_param_combine(&mut self) {
        self.gold_combine = 0;
        self.gem_combine = 0;
        self.ratio_combine = 0.0;
        self.count_da_nang_cap = 0;
        self.count_da_bao_ve = 0;
    }

    pub fn dispose(&mut self) {
        self.items_combine.clear();
    }

    pub fn set_type_combine(&mut self, type_combine: CombineType) {
        self.type_combine = type_combine;
    }
}
