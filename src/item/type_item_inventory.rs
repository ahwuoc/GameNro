pub enum TypeItemInventory {
    BoxToBodyOrBag ,
    BagToBox ,
    BodyToBox,
    BagToBody,
    BodyToBag,
}
impl TryFrom<i8> for TypeItemInventory{
    type Error= anyhow::Error;
    fn try_from(value: i8) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(Self::BoxToBodyOrBag),
            1 => Ok(Self::BagToBox),
            3 => Ok(Self::BodyToBox),
            4 => Ok(Self::BagToBody),
            5 => Ok(Self::BodyToBag),
            _ => Err(anyhow::anyhow!("Invalid type error {}",value))
        }
    }
}
