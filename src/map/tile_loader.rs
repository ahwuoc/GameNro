use std::fs;

pub struct TileLoader;

impl TileLoader {
    pub fn read_tile_map_file(map_id: i32) -> Option<(i32, i32, Vec<Vec<i32>>)> {
        let path = format!("data/girlkun/map/tile_map_data/{}", map_id);
        let data = fs::read(&path).ok()?;
        if data.len() < 2 { return None; }
        let w = data[0] as usize;
        let h = data[1] as usize;
        let expected = 2 + w * h;
        if data.len() < expected { return None; }
        let mut tiles: Vec<Vec<i32>> = Vec::with_capacity(h);
        let mut idx = 2;
        for _row in 0..h {
            let mut row: Vec<i32> = Vec::with_capacity(w);
            for _col in 0..w {
                row.push(data[idx] as i32);
                idx += 1;
            }
            tiles.push(row);
        }
        Some((w as i32, h as i32, tiles))
    }

    pub fn read_tile_top_file(tile_id: i32) -> Option<Vec<i32>> {
        let path = format!("data/girlkun/map/tile_top/{}", tile_id);
        let data = fs::read(&path).ok()?;
        Some(data.into_iter().map(|b| b as i32).collect())
    }

    pub fn load_tile_data(map_id: i32, tile_id: i32) -> Option<TileData> {
        let tile_map = Self::read_tile_map_file(map_id)?;
        let tile_top = Self::read_tile_top_file(tile_id);
        
        Some(TileData {
            width: tile_map.0,
            height: tile_map.1,
            tile_map: tile_map.2,
            tile_top: tile_top.unwrap_or_default(),
        })
    }

    pub fn validate_tile_position(tile_data: &TileData, x: i32, y: i32) -> bool {
        x >= 0 && x < tile_data.width && y >= 0 && y < tile_data.height
    }

    pub fn get_tile_at_position(tile_data: &TileData, x: i32, y: i32) -> Option<i32> {
        if Self::validate_tile_position(tile_data, x, y) {
            Some(tile_data.tile_map[y as usize][x as usize])
        } else {
            None
        }
    }

    pub fn is_walkable_tile(tile_id: i32) -> bool {
        // Define walkable tile IDs
        let walkable_tiles = vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
        walkable_tiles.contains(&tile_id)
    }

    pub fn is_water_tile(tile_id: i32) -> bool {
        // Define water tile IDs
        let water_tiles = vec![10, 11, 12, 13, 14, 15];
        water_tiles.contains(&tile_id)
    }

    pub fn is_wall_tile(tile_id: i32) -> bool {
        // Define wall tile IDs
        let wall_tiles = vec![20, 21, 22, 23, 24, 25];
        wall_tiles.contains(&tile_id)
    }

    pub fn get_tile_type(tile_id: i32) -> TileType {
        if Self::is_walkable_tile(tile_id) {
            TileType::Walkable
        } else if Self::is_water_tile(tile_id) {
            TileType::Water
        } else if Self::is_wall_tile(tile_id) {
            TileType::Wall
        } else {
            TileType::Unknown
        }
    }
}

#[derive(Debug, Clone)]
pub struct TileData {
    pub width: i32,
    pub height: i32,
    pub tile_map: Vec<Vec<i32>>,
    pub tile_top: Vec<i32>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum TileType {
    Walkable,
    Water,
    Wall,
    Unknown,
}
