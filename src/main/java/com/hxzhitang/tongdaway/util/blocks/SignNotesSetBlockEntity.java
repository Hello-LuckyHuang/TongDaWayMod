package com.hxzhitang.tongdaway.util.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class SignNotesSetBlockEntity extends BlockEntity {
    private boolean isActivation = false;
    private Map<ScrollText[], SignMetaData> scrollText = new HashMap<>();

    private int tick = 0;

    public SignNotesSetBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(ModBlockEntities.SIGN_NOTES_SET.get(), p_155229_, p_155230_);
    }

    public boolean anyPlayerInRange() {
        return Objects.requireNonNull(this.getLevel()).hasNearbyAlivePlayer(this.getBlockPos().getX() + 0.5D, this.getBlockPos().getY() + 0.5D, this.getBlockPos().getZ() + 0.5D, 50);
    }

    //静态方法，每次回调都会更新状态
    public static void tick(Level level, BlockPos blockPos, BlockState state, SignNotesSetBlockEntity entity) {
        if (!entity.isActivation)
            return;
        if (!entity.anyPlayerInRange())
            return;

        if (!level.isClientSide) {
            if (entity.tick % 20 == 0) {
                for (ScrollText[] scrollTexts : entity.scrollText.keySet()) {
                    SignMetaData signMetaData = entity.scrollText.get(scrollTexts);
                    BlockEntity blockEntity = level.getBlockEntity(blockPos.offset(signMetaData.signOffsetPos));
                    if (blockEntity instanceof SignBlockEntity sign) {
                        //根据告示牌和悬挂告示牌区分显示字符数
                        int lineCharacterNum = sign instanceof HangingSignBlockEntity ? 10 : 14;
                        for (int i = 0; i < 4; i++) {
                            ScrollText scrollText = scrollTexts[i];
                            int showLength = scrollText.text.matches(".*[^\\x00-\\x7F].*") ? lineCharacterNum-5 : lineCharacterNum;
                            if (scrollText.text.length() > showLength) {
                                String showText = "                   ".substring(0, showLength) + scrollText.text + "                   ";
                                sign.setText((signMetaData.inFront ? sign.getFrontText() : sign.getBackText()).setMessage(i, Component.literal(showText.substring(scrollText.index, scrollText.index + showLength))), signMetaData.inFront);
                                scrollText.index = scrollText.index + 1 > scrollText.text.length() + showLength ? 0 : scrollText.index + 1;
                            } else {
                                sign.setText((signMetaData.inFront ? sign.getFrontText() : sign.getBackText()).setMessage(i, Component.literal(scrollText.text)), signMetaData.inFront);
                            }
                        }
                    }
                }
            }
            entity.tick++;
        }
    }

//    public void onLoad() {
//        super.onLoad();
//        this.setActivation(true);
//    }

    // 存储实体的数据
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("isActivation", isActivation? 1 : 0);
        ListTag signsList = new ListTag();
        for (ScrollText[] scrollTexts : this.scrollText.keySet()) {
            CompoundTag scrollText = new CompoundTag();
            ListTag scrollTextList = new ListTag();
            for (ScrollText st : scrollTexts) {
                scrollTextList.add(StringTag.valueOf(st.text));
            }
            SignMetaData signMetaData = this.scrollText.get(scrollTexts);
            CompoundTag signMetaDataTag = new CompoundTag();
            signMetaDataTag.put("inFront", IntTag.valueOf(signMetaData.inFront? 1 : 0));
            signMetaDataTag.put("x", IntTag.valueOf(signMetaData.signOffsetPos.getX()));
            signMetaDataTag.put("y", IntTag.valueOf(signMetaData.signOffsetPos.getY()));
            signMetaDataTag.put("z", IntTag.valueOf(signMetaData.signOffsetPos.getZ()));
            scrollText.put("signMetaData", signMetaDataTag);
            scrollText.put("scrollText", scrollTextList);
            signsList.add(scrollText);
        }
        nbt.put("signsList", signsList);
        super.saveAdditional(nbt);
    }

    // 读取实体的数据
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isActivation = nbt.getInt("isActivation") == 1;
        ListTag signsList = (ListTag) nbt.get("signsList");
        if (signsList != null) {
            for (Tag tag : signsList) {
                CompoundTag scrollTextTag = (CompoundTag) tag;
                ListTag scrollTextList = (ListTag) scrollTextTag.get("scrollText");
                ScrollText[] scrollTexts = new ScrollText[4];
                if (scrollTextList != null) {
                    int i = 0;
                    for (Tag tag1 : scrollTextList) {
                        scrollTexts[i++] = new ScrollText(tag1.getAsString(), 0);
                    }
                }
                CompoundTag signMetaDataTag = (CompoundTag) scrollTextTag.get("signMetaData");
                if (signMetaDataTag!= null) {
                    int x = signMetaDataTag.getInt("x");
                    int y = signMetaDataTag.getInt("y");
                    int z = signMetaDataTag.getInt("z");
                    boolean inFront = signMetaDataTag.getInt("inFront") == 1;
                    var signMetaData = new SignMetaData(new BlockPos(x, y, z), inFront);
                    scrollText.put(scrollTexts, signMetaData);
                }
            }
        }
    }

    //在被移除时在控制的木牌上生成“错误”信息
    public void onRemove(BlockPos blockPos) {
        List<String[]> removeNotes = List.of(
                new String[]{"xx**...&**x", "*x**..x*.*x", "xxxx....**...**", "x*x*..&&...*"},
                new String[]{"§cError:  ", "§cWE LOSS ", "§cCONNECT ", "§cTO DATA."},
                new String[]{"???...??", "...???...", "?..??..???", "??...???.?"},
                new String[]{"§1:(      ", "§1You sign", "§1ran into", "§1a problem"},
                new String[]{"§f■■■■■■■■", "§f■■_■■_■■", "§f■■■■■■■■", "§l__, __!"},
                new String[]{"§8■■□■□□■□", "§8■□■□■□■□", "§8□■■□■□□■", "§8■□■□□■■□"},
                new String[]{"发生甚么事了?", "我说怎么回事?", "原来是昨天", "谁改我键位了?"},
                new String[]{"§lox0o0ox0oo", "§lx00oo0x0xo", "§l0xoxo0xo0x", "§l00xoo0xoxx"},
                new String[]{"锟斤拷烫烫烫锟斤拷", "烫烫烫烫烫烫烫烫烫", "烫烫锟斤拷烫烫锟斤", "斤拷锟斤拷锟斤拷锟"}
        );

        Set<SignMetaData> signMetaDatas = new HashSet<>();
        for (ScrollText[] scrollTexts : scrollText.keySet()) {
            signMetaDatas.add(scrollText.get(scrollTexts));
        }
        for (SignMetaData signMetaData : signMetaDatas) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos.offset(signMetaData.signOffsetPos));
            if (blockEntity instanceof SignBlockEntity sign) {
                Random random = new Random();
                int r = random.nextInt(removeNotes.size());
                String[] removeNote = removeNotes.get(r);
                for (int i = 0; i < 4; i++) {
                    sign.setText((signMetaData.inFront ? sign.getFrontText() : sign.getBackText()).setMessage(i, Component.literal(removeNote[i])), signMetaData.inFront);
                }
            }
        }
    }

    public void setScrollText(String notes, SignMetaData... signMetaDatas) {
        Arrays.sort(signMetaDatas, (md1, md2) -> {
            if (md1.signOffsetPos.getY() == md2.signOffsetPos.getY())
                return md1.signOffsetPos.getX() - md2.signOffsetPos.getX();
            else if (md1.signOffsetPos.getX() == md2.signOffsetPos.getX()) {
                return (md1.inFront?1:0)-(md2.inFront?1:0); // 按照 inFront 字段排序，true 在前 (1)，false 在后 (0)
            }
            return md1.signOffsetPos.getY() - md2.signOffsetPos.getY();
        });
        var notesArray = notes.split("\n");
        int i = 0;
        for (SignMetaData signMetaData : signMetaDatas) {
            String[] text = {"", "", "", ""};
            if (i*4 >= notesArray.length)
                break;
            else if (i*4 + 4 >= notesArray.length) {
                System.arraycopy(notesArray, i*4, text, 0, notesArray.length-i*4);
                i++;
            } else
                System.arraycopy(notesArray, (i++)*4, text, 0, 4);
            scrollText.put(new ScrollText[]{new ScrollText(text[0], 0), new ScrollText(text[1], 0), new ScrollText(text[2], 0), new ScrollText(text[3], 0)}, signMetaData);
        }
    }

    public void setActivation(boolean activation) {
        isActivation = activation;
    }

    public record SignMetaData(BlockPos signOffsetPos, boolean inFront) {
    }

    public class ScrollText {
        public String text;
        public int index;

        public ScrollText(String text, int index) {
            this.text = text;
            this.index = index;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ScrollText scrollText = (ScrollText) o;
            return scrollText.text.equals(text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }
    }
}
