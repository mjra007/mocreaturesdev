package drzhark.mocreatures.entity;

import drzhark.mocreatures.MoCTools;
import drzhark.mocreatures.MoCreatures;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import java.util.List;

public abstract class MoCEntityAquatic extends EntityWaterMob implements IMoCEntity//, IEntityAdditionalSpawnData
{

    protected boolean divePending;
    protected boolean jumpPending;
    protected boolean isEntityJumping;
    private PathEntity pathEntity;
    private int outOfWater;
    private int maxHealth;
    private boolean diving;
    private int divingCount;
    private int mountCount;
    public boolean fishHooked;
    protected boolean riderIsDisconnecting;
    protected float moveSpeed;
    protected String texture;

    public MoCEntityAquatic(World world) {
        super(world);
        this.outOfWater = 0;
        setTamed(false);
        setTemper(50);
        this.riderIsDisconnecting = false;
        this.texture = "blank.jpg";
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(getMoveSpeed());
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(6.0D);
    }

    @Override
    public ResourceLocation getTexture() {
        return MoCreatures.proxy.getTexture(this.texture);
    }

    @Override
    public IEntityLivingData onSpawnFirstTime(DifficultyInstance difficulty, IEntityLivingData par1EntityLivingData) {
        selectType();
        return super.onSpawnFirstTime(difficulty, par1EntityLivingData);
    }

    /**
     * Put your code to choose a texture / the mob type in here. Will be called
     * by default MocEntity constructors.
     */
    @Override
    public void selectType() {
        setType(1);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, Byte.valueOf((byte) 0)); // byte IsTamed, 0 = false 1 = true
        this.dataWatcher.addObject(17, String.valueOf("")); // Name empty string by default
        this.dataWatcher.addObject(18, Byte.valueOf((byte) 0)); // byte IsAdult, 0 = false 1 = true
        this.dataWatcher.addObject(19, Integer.valueOf(0)); // int ageTicks / "edad"
        this.dataWatcher.addObject(20, Integer.valueOf(0)); // integer type - will be automatically checked and networked in onUpdate-EntityLiving
        this.dataWatcher.addObject(21, String.valueOf("")); //owners name
        this.dataWatcher.addObject(22, Integer.valueOf(50)); // int temper
    }

    public int getTemper() {
        return this.dataWatcher.getWatchableObjectInt(22);
    }

    @Override
    public boolean getIsTamed() {
        return (this.dataWatcher.getWatchableObjectByte(16) == 1);
    }

    @Override
    public String getName() {
        return this.dataWatcher.getWatchableObjectString(17);
    }

    @Override
    public boolean getIsAdult() {
        return (this.dataWatcher.getWatchableObjectByte(18) == 1);
    }

    /**
     * @return networked Entity "Age" in integer value, typical values are
     *         0-100. Old float eDad was typically 0F-1.0F
     */
    @Override
    public int getEdad() {
        return this.dataWatcher.getWatchableObjectInt(19);
    }

    @Override
    public int getType() {
        return this.dataWatcher.getWatchableObjectInt(20);
    }

    public void setTemper(int i) {
        this.dataWatcher.updateObject(22, Integer.valueOf(i));
    }

    @Override
    public void setTamed(boolean flag) {
        byte input = (byte) (flag ? 1 : 0);
        this.dataWatcher.updateObject(16, Byte.valueOf(input));
    }

    @Override
    public void setName(String name) {
        this.dataWatcher.updateObject(17, String.valueOf(name));
    }

    @Override
    public void setAdult(boolean flag) {
        byte input = (byte) (flag ? 1 : 0);
        this.dataWatcher.updateObject(18, Byte.valueOf(input));
    }

    @Override
    public void setEdad(int i) {
        this.dataWatcher.updateObject(19, Integer.valueOf(i));
    }

    @Override
    public void setType(int i) {
        this.dataWatcher.updateObject(20, Integer.valueOf(i));
    }

    /**
     * How difficult is the creature to be tamed? the Higher the number, the
     * more difficult
     */
    public int getMaxTemper() {
        return 100;
    }

    public float b(float f, float f1, float f2) {
        float f3 = f1;
        for (f3 = f1 - f; f3 < -180F; f3 += 360F) {
        }
        for (; f3 >= 180F; f3 -= 360F) {
        }
        if (f3 > f2) {
            f3 = f2;
        }
        if (f3 < -f2) {
            f3 = -f2;
        }
        return f + f3;
    }

    public void faceItem(int i, int j, int k, float f) {
        double d = i - this.posX;
        double d1 = k - this.posZ;
        double d2 = j - this.posY;
        double d3 = MathHelper.sqrt_double((d * d) + (d1 * d1));
        float f1 = (float) ((Math.atan2(d1, d) * 180D) / 3.1415927410125728D) - 90F;
        float f2 = (float) ((Math.atan2(d2, d3) * 180D) / 3.1415927410125728D);
        this.rotationPitch = -b(this.rotationPitch, f2, f);
        this.rotationYaw = b(this.rotationYaw, f1, f);
    }

    @Override
    protected boolean canDespawn() {
        if (MoCreatures.proxy.forceDespawns) {
            return !getIsTamed();
        } else {
            return false;
        }
    }

    @Override
    public boolean checkSpawningBiome() {
        return true;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block par4) {
        // TODO make the sounds
    }

    @Override
    public void fall(float f, float f1) {
    }

    public EntityItem getClosestFish(Entity entity, double d) {
        double d1 = -1D;
        EntityItem entityitem = null;
        List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().expand(d, d, d));
        for (int i = 0; i < list.size(); i++) {
            Entity entity1 = (Entity) list.get(i);
            if (!(entity1 instanceof EntityItem)) {
                continue;
            }
            EntityItem entityitem1 = (EntityItem) entity1;
            if ((entityitem1.getEntityItem().getItem() != Items.fish) || !entityitem1.isInWater()) {
                continue;
            }
            double d2 = entityitem1.getDistanceSq(entity.posX, entity.posY, entity.posZ);
            if (((d < 0.0D) || (d2 < (d * d))) && ((d1 == -1D) || (d2 < d1))) {
                d1 = d2;
                entityitem = entityitem1;
            }
        }

        return entityitem;
    }

    @Override
    protected String getDeathSound() {
        return null;
    }

    @Override
    protected String getHurtSound() {
        return null;
    }

    @Override
    protected String getLivingSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    public boolean gettingOutOfWater() {
        int i = (int) this.posX;
        int j = (int) this.posY;
        int k = (int) this.posZ;
        return this.worldObj.isAirBlock(new BlockPos(i, j + 1, k));
    }

    protected String getUpsetSound() {
        return null;
    }

    /**
     * mount jumping power
     */
    public double getCustomJump() {
        return 0.4D;
    }

    public void setIsJumping(boolean flag) {
        this.isEntityJumping = flag;
    }

    public boolean getIsJumping() {
        return this.isEntityJumping;
    }

    public boolean getRenderName() {
        return (getName() != null && !getName().equals(""));
    }

    /**
     * Sets a flag that will make the Entity "jump" in the next onGround
     * moveEntity update
     */
    @Override
    public void makeEntityJump() {
        this.jumpPending = true;
    }

    @Override
    public boolean handleWaterMovement() {
        return this.worldObj.handleMaterialAcceleration(this.getEntityBoundingBox(), Material.water, this);
    }

    @Override
    public void moveEntityWithHeading(float f, float f1) {
        if (this.riddenByEntity == null) {
            super.moveEntityWithHeading(f, f1);
        }
        float par1 = f;
        float par2 = f1;

        if ((this.riddenByEntity != null) && !getIsTamed() && !isSwimming()) {
            this.riddenByEntity.mountEntity(null);
            return;
        }

        if ((this.riddenByEntity != null) && !getIsTamed()) {
            if ((this.rand.nextInt(5) == 0) && !getIsJumping() && this.jumpPending) {
                this.motionY += getCustomJump();
                setIsJumping(true);
                this.jumpPending = false;
            }
            if (this.rand.nextInt(10) == 0) {
                this.motionX += this.rand.nextDouble() / 30D;
                this.motionZ += this.rand.nextDouble() / 10D;
            }
            if (MoCreatures.isServer()) {
                moveEntity(this.motionX, this.motionY, this.motionZ);
            }
            if (MoCreatures.isServer() && this.rand.nextInt(50) == 0) {
                if (getUpsetSound() != null) {
                    this.worldObj.playSoundAtEntity(this, getUpsetSound(), 1.0F, 1.0F + ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F));
                }
                this.riddenByEntity.motionY += 0.9D;
                this.riddenByEntity.motionZ -= 0.3D;
                this.riddenByEntity.mountEntity(null);
                this.ridingEntity = null;
            }
            if (this.onGround) {
                setIsJumping(false);
            }
            if (MoCreatures.isServer() && this instanceof IMoCTameable) {
                int chance = (getMaxTemper() - getTemper());
                if (chance <= 0) {
                    chance = 1;
                }
                if (this.rand.nextInt(chance * 8) == 0) {
                    MoCTools.tameWithName((EntityPlayer) this.riddenByEntity, (IMoCTameable) this);
                }

            }
        } else if ((this.riddenByEntity != null) && getIsTamed())// && isSwimming())
        {
            this.motionX += this.riddenByEntity.motionX * (getCustomSpeed() / 5D);
            this.motionZ += this.riddenByEntity.motionZ * (getCustomSpeed() / 5D);
            par1 = ((EntityLivingBase) this.riddenByEntity).moveStrafing * 0.5F;
            par2 = ((EntityLivingBase) this.riddenByEntity).moveForward;

            if (this.jumpPending) {
                this.motionY += getCustomJump();
                this.jumpPending = false;
            }

            if (this.divePending) {
                this.divePending = false;
                this.motionY -= 0.3D;
            }

            if (this.motionY > 0.01D && !isSwimming()) {
                this.motionY = -0.01D;
            }
            this.rotationPitch = this.riddenByEntity.rotationPitch * 0.5F;
            this.prevRotationYaw = this.rotationYaw = this.riddenByEntity.rotationYaw;
            setRotation(this.rotationYaw, this.rotationPitch);

            if (MoCreatures.isServer()) {
                //moveEntity(motionX, motionY, motionZ);
                super.moveEntityWithHeading(par1, par2);
            }

            this.motionX *= 0.95D;
            this.motionZ *= 0.95D;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d2 = this.posX - this.prevPosX;
        double d3 = this.posZ - this.prevPosZ;
        float f4 = MathHelper.sqrt_double((d2 * d2) + (d3 * d3)) * 4.0F;
        if (f4 > 1.0F) {
            f4 = 1.0F;
        }

        this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    protected boolean MoveToNextEntity(Entity entity) {
        if (entity != null) {
            int i = MathHelper.floor_double(entity.posX);
            int j = MathHelper.floor_double(entity.posY);
            int k = MathHelper.floor_double(entity.posZ);
            faceItem(i, j, k, 30F);
            if (this.posX < i) {
                double d = entity.posX - this.posX;
                if (d > 0.5D) {
                    this.motionX += 0.050000000000000003D;
                }
            } else {
                double d1 = this.posX - entity.posX;
                if (d1 > 0.5D) {
                    this.motionX -= 0.050000000000000003D;
                }
            }
            if (this.posZ < k) {
                double d2 = entity.posZ - this.posZ;
                if (d2 > 0.5D) {
                    this.motionZ += 0.050000000000000003D;
                }
            } else {
                double d3 = this.posZ - entity.posZ;
                if (d3 > 0.5D) {
                    this.motionZ -= 0.050000000000000003D;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Speed used to move the mob around
     *
     * @return
     */
    public double getCustomSpeed() {
        return 1.5D;
    }

    @Override
    public boolean isInWater() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean isDiving() {
        return this.diving;
    }

    @Override
    protected void jump() {

    }

    public void floating() {
        float distY = MoCTools.distanceToSurface(this);

        if (this.riddenByEntity != null) {
            EntityPlayer ep = (EntityPlayer) this.riddenByEntity;
            if (ep.isAirBorne) // TODO TEST
            {
                this.motionY += 0.09D;
            } else {
                this.motionY = -0.008D;
            }
            return;
        }

        if ((getAttackTarget() != null && ((getAttackTarget().posY < (this.posY - 0.5D)) && getDistanceToEntity(getAttackTarget()) < 10F))) {
            if (this.motionY < -0.1) {
                this.motionY = -0.1;
            }
            return;
        }

        if (distY < 1 || isDiving()) {
            if (this.motionY < -0.05) {
                this.motionY = -0.05;
            }
        } else {
            if (this.motionY < 0) {
                this.motionY = 0;
            }
            this.motionY += 0.001D;// 0.001

            if (distY > 1) {
                this.motionY += (distY * 0.02);
                if (this.motionY > 0.2D) {
                    this.motionY = 0.2D;
                }
            }
        }
    }

    // used to pick up objects while riding an entity
    public void Riding() {
        if ((this.riddenByEntity != null) && (this.riddenByEntity instanceof EntityPlayer)) {
            EntityPlayer entityplayer = (EntityPlayer) this.riddenByEntity;
            List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().expand(1.0D, 0.0D, 1.0D));
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Entity entity = (Entity) list.get(i);
                    if (entity.isDead) {
                        continue;
                    }
                    entity.onCollideWithPlayer(entityplayer);
                    if (!(entity instanceof EntityMob)) {
                        continue;
                    }
                    float f = getDistanceToEntity(entity);
                    if ((f < 2.0F) && entity instanceof EntityMob && (this.rand.nextInt(10) == 0)) {
                        attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase) entity),
                                (float) ((EntityMob) entity).getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
                    }
                }
            }
            /*
             if (entityplayer.isSneaking()) {
                 this.makeEntityDive();
             }
             */
        }
    }

    @Override
    public boolean isMovementCeased() {
        return ((!isSwimming() && this.riddenByEntity == null) || this.riddenByEntity != null || this.getIsSitting());
    }

    @Override
    public void onLivingUpdate() {
        if (MoCreatures.isServer()) {
            if (this.riddenByEntity != null) {
                Riding();
                this.mountCount = 1;
            }

            if (this.mountCount > 0) {
                if (++this.mountCount > 50) {
                    this.mountCount = 0;
                }
            }

            if (isMovementCeased()) {
                this.getNavigator().clearPathEntity();
            }
            /*
             if (getIsTamed() && rand.nextInt(100) == 0) {
                 MoCServerPacketHandler.sendHealth(this.getEntityId(),
                 this.worldObj.provider.getDimensionId(), this.getHealth());
             }
             */

            if (forceUpdates() && this.rand.nextInt(500) == 0) {
                MoCTools.forceDataSync(this);
            }

            if (isFisheable() && !this.fishHooked && this.rand.nextInt(30) == 0) {
                getFished();
            }

            if (this.fishHooked && this.rand.nextInt(200) == 0) {
                this.fishHooked = false;

                List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().expand(2, 2, 2));
                for (int i = 0; i < list.size(); i++) {
                    Entity entity1 = (Entity) list.get(i);

                    if (entity1 instanceof EntityFishHook) {
                        if (((EntityFishHook) entity1).caughtEntity == this) {
                            ((EntityFishHook) entity1).caughtEntity = null;
                        }
                    }
                }
            }
        }

        // TODO
        /*
        if (isNotScared() && fleeingTick > 0) {
            fleeingTick = 0;
        }*/

        this.moveSpeed = getMoveSpeed();

        if (isSwimming()) {
            floating();
            this.outOfWater = 0;
            this.setAir(300);
        } else {
            if (this.riddenByEntity != null) {
                if (this.riddenByEntity.isSneaking()) {
                    this.riddenByEntity.mountEntity(null);
                }
            }

            this.outOfWater++;
            if (this.outOfWater > 10) {
                this.getNavigator().clearPathEntity();
            }
            if (this.outOfWater > 100 && (this.outOfWater % 20) == 0) {
                this.motionY += 0.3D;
                this.motionX = (float) (Math.random() * 0.2D - 0.1D);
                this.motionZ = (float) (Math.random() * 0.2D - 0.1D);
                attackEntityFrom(DamageSource.drown, 1);
            }
        }

        // TODO
        /*if (!hasPath() && this.riddenByEntity == null && !isMovementCeased() && getAttackTarget() == null) {
            updateWanderPath();
        }*/

        if (!this.diving) {
            if (this.riddenByEntity == null && getAttackTarget() == null && !this.navigator.noPath() && this.rand.nextInt(500) == 0) {
                this.diving = true;
            }
        } else {
            this.divingCount++;
            if (this.divingCount > 100 || this.riddenByEntity != null) {
                this.diving = false;
                this.divingCount = 0;
            }
        }
        super.onLivingUpdate();
    }

    public boolean isSwimming() {
        return ((isInsideOfMaterial(Material.water)));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        super.writeEntityToNBT(nbttagcompound);
        nbttagcompound.setBoolean("Tamed", getIsTamed());
        nbttagcompound.setBoolean("Adult", getIsAdult());
        nbttagcompound.setInteger("Edad", getEdad());
        nbttagcompound.setString("Name", getName());
        nbttagcompound.setInteger("TypeInt", getType());
        nbttagcompound.setString("Owner", getOwnerName());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        super.readEntityFromNBT(nbttagcompound);
        setTamed(nbttagcompound.getBoolean("Tamed"));
        setAdult(nbttagcompound.getBoolean("Adult"));
        setEdad(nbttagcompound.getInteger("Edad"));
        setName(nbttagcompound.getString("Name"));
        setType(nbttagcompound.getInteger("TypeInt"));
        setOwner(nbttagcompound.getString("Owner"));
    }

    /**
     * fixes dewspawning tamed creatures
     */
    @Override
    public boolean isEntityInsideOpaqueBlock() {
        if (getIsTamed()) {
            return false;
        }

        return super.isEntityInsideOpaqueBlock();
    }

    public void setDisplayName(boolean flag) {
    }

    public void setTypeInt(int i) {
        setType(i);
        selectType();
    }

    /**
     * Used to synchronize the attack animation between server and client
     *
     * @param attackType
     */
    @Override
    public void performAnimation(int attackType) {
    }

    /**
     * Makes the entity despawn if requirements are reached changed to the
     * entities now last longer
     */
    @Override
    protected void despawnEntity() {
        EntityPlayer var1 = this.worldObj.getClosestPlayerToEntity(this, -1.0D);
        if (var1 != null) {
            double var2 = var1.posX - this.posX;
            double var4 = var1.posY - this.posY;
            double var6 = var1.posZ - this.posZ;
            double var8 = var2 * var2 + var4 * var4 + var6 * var6;

            if (this.canDespawn() && var8 > 16384.0D) {
                this.setDead();
            }
            //changed from 600
            if (this.entityAge > 1800 && this.rand.nextInt(800) == 0 && var8 > 1024.0D && this.canDespawn()) {
                this.setDead();
            } else if (var8 < 1024.0D) {
                this.entityAge = 0;
            }
        }
    }

    public float getMoveSpeed() {
        return 0.7F;
    }

    @Override
    public int nameYOffset() {
        return 0;
    }

    @Override
    public boolean renderName() {
        return false;
    }

    @Override
    public boolean updateMount() {
        return false;
    }

    @Override
    public boolean forceUpdates() {
        return false;
    }

    @Override
    public void makeEntityDive() {
        this.divePending = true;
    }

    @Override
    public float getSizeFactor() {
        return 1.0F;
    }

    @Override
    public float getAdjustedYOffset() {
        return 0F;
    }

    /**
     * Checks if the entity's current position is a valid location to spawn this
     * entity.
     */
    @Override
    public boolean getCanSpawnHere() {
        return MoCreatures.entityMap.get(this.getClass()).getFrequency() > 0 && this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox());
    }

    @Override
    public String getOwnerName() {
        return this.dataWatcher.getWatchableObjectString(21);
    }

    @Override
    public void setOwner(String par1Str) {
        this.dataWatcher.updateObject(21, par1Str);
    }

    @Override
    public boolean attackEntityFrom(DamageSource damagesource, float i) {
        Entity entity = damagesource.getEntity();
        //this avoids damage done by Players to a tamed creature that is not theirs
        if (MoCreatures.proxy.enableOwnership && getOwnerName() != null && !getOwnerName().equals("") && entity != null
                && entity instanceof EntityPlayer && !((EntityPlayer) entity).getCommandSenderName().equals(getOwnerName())
                && !MoCTools.isThisPlayerAnOP(((EntityPlayer) entity))) {
            return false;
        }

        //to prevent tamed aquatics from getting block damage
        if (getIsTamed() && damagesource.getDamageType().equalsIgnoreCase("inWall")) {
            return false;
        }
        /*
         * if (MoCreatures.isServer() && getIsTamed()) {
         * //MoCServerPacketHandler.sendHealth(this.getEntityId(),
         * this.worldObj.provider.getDimensionId(), this.getHealth()); }
         */

        return super.attackEntityFrom(damagesource, i);

    }

    protected boolean canBeTrappedInNet() {
        return (this instanceof IMoCTameable) && getIsTamed();
    }

    protected void dropMyStuff() {
        // TODO Auto-generated method stub
    }

    /**
     * Used to heal the animal
     *
     * @param itemstack
     * @return
     */
    protected boolean isMyHealFood(ItemStack itemstack) {
        return false;
    }

    @Override
    public void setArmorType(byte i) {
    }

    @Override
    public void dismountEntity() {
        if (MoCreatures.isServer() && this.riddenByEntity != null) {
            this.riddenByEntity.mountEntity(null);
            this.riddenByEntity = null;
        }
    }

    @Override
    public int pitchRotationOffset() {
        return 0;
    }

    @Override
    public int rollRotationOffset() {
        return 0;
    }

    /**
     * The act of getting Hooked into a fish Hook.
     */
    private void getFished() {
        EntityPlayer entityplayer1 = this.worldObj.getClosestPlayerToEntity(this, 18D);
        if (entityplayer1 != null) {
            EntityFishHook fishHook = entityplayer1.fishEntity;
            if (fishHook != null && fishHook.caughtEntity == null) {
                float f = fishHook.getDistanceToEntity(this);
                if (f > 1) {
                    MoCTools.getPathToEntity(this, fishHook, f);
                } else {
                    fishHook.caughtEntity = this;
                    this.fishHooked = true;
                }
            }
        }
    }

    /**
     * Is this aquatic entity prone to be fished with a fish Hook?
     *
     * @return
     */
    protected boolean isFisheable() {
        return false;
    }

    @Override
    public int yawRotationOffset() {
        return 0;
    }

    @Override
    public float getAdjustedZOffset() {
        return 0F;
    }

    @Override
    public float getAdjustedXOffset() {
        return 0F;
    }

    /**
     * Finds and entity described in entitiesToInclude at d distance
     *
     * @param d
     * @return
     */
    protected EntityLivingBase getBoogey(double d) {
        double d1 = -1D;
        EntityLivingBase entityliving = null;
        List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().expand(d, 4D, d));
        for (int i = 0; i < list.size(); i++) {
            Entity entity = (Entity) list.get(i);
            if (entitiesToInclude(entity)) {
                entityliving = (EntityLivingBase) entity;
            }
        }
        return entityliving;
    }

    /**
     * Used in getBoogey to specify what kind of entity to look for
     *
     * @param entity
     * @return
     */
    public boolean entitiesToInclude(Entity entity) {
        return ((entity.getClass() != this.getClass()) && (entity instanceof EntityLivingBase) && ((entity.width >= 0.5D) || (entity.height >= 0.5D)));
    }

    public boolean isNotScared() {
        return false;
    }

    @Override
    public void riderIsDisconnecting(boolean flag) {
        this.riderIsDisconnecting = true;
    }

    @Override
    public boolean canAttackTarget(EntityLivingBase entity) {
        return false;
    }

    @Override
    public boolean allowLeashing() {
        return false;
    }

    @Override
    public boolean getIsSitting() {
        return false;
    }
}
