package xyz.pyrehaven.realsize.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** The vanilla loops need a failure edge after finalization, not another early check. */
public final class SpawnPlacementPlugin implements IMixinConfigPlugin {
    private static final String NATURAL = "net.minecraft.world.level.NaturalSpawner";
    private static final String MOB = "net/minecraft/world/entity/Mob";
    private static final String GROUP = "Lnet/minecraft/world/entity/SpawnGroupData;";
    private static final String FINALIZE = "(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;"
            + "Lnet/minecraft/world/entity/EntitySpawnReason;" + GROUP + ")" + GROUP;

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!target.equals(NATURAL)) return;
        int patched = 0;
        for (var method : node.methods) {
            if (!method.name.equals("spawnCategoryForPosition") && !method.name.equals("spawnMobsForChunkGeneration")) continue;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(MOB)
                        && call.name.equals("finalizeSpawn") && call.desc.equals(FINALIZE)) {
                    addRejectionEdge(method, call);
                    patched++;
                }
            }
        }
        if (patched != 2) throw new IllegalStateException("RealSize expected two natural finalization boundaries, found " + patched);
    }

    private static void addRejectionEdge(MethodNode method, MethodInsnNode call) {
        AbstractInsnNode next = executable(call.getNext());
        if (!(next instanceof VarInsnNode store) || store.getOpcode() != Opcodes.ASTORE) {
            throw new IllegalStateException("RealSize finalization result is not a direct group assignment");
        }
        AbstractInsnNode before = call.getPrevious();
        while (before != null && !(before instanceof JumpInsnNode)) before = before.getPrevious();
        if (!(before instanceof JumpInsnNode guard)
                || (guard.getOpcode() != Opcodes.IFEQ && guard.getOpcode() != Opcodes.GOTO)) {
            throw new IllegalStateException("RealSize finalization has no candidate obstruction failure branch");
        }
        AbstractInsnNode start = executable(guard.getNext());
        AbstractInsnNode level = executable(start.getNext());
        if (!(start instanceof VarInsnNode mobLoad) || mobLoad.getOpcode() != Opcodes.ALOAD
                || !(level instanceof VarInsnNode levelLoad) || levelLoad.getOpcode() != Opcodes.ALOAD) {
            throw new IllegalStateException("RealSize finalization receiver/level locals changed");
        }
        // Existing failure target already has a stack-map frame with an empty stack.
        // A new temporary keeps the uncommitted result off that edge, without
        // introducing a branch label or changing the original group on rejection.
        int temporary = method.maxLocals++;
        var begin = new InsnList();
        begin.add(new VarInsnNode(Opcodes.ALOAD, mobLoad.var));
        begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/pyrehaven/realsize/sizing/SpawnPreparation", "begin",
                "(Lnet/minecraft/world/entity/Mob;)V", false));
        method.instructions.insertBefore(start, begin);
        var edge = new InsnList();
        edge.add(new VarInsnNode(Opcodes.ASTORE, temporary));
        edge.add(new VarInsnNode(Opcodes.ALOAD, levelLoad.var));
        edge.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "xyz/pyrehaven/realsize/sizing/SpawnPreparation", "finish",
                "(Lnet/minecraft/world/level/ServerLevelAccessor;)Z", false));
        edge.add(new JumpInsnNode(Opcodes.IFEQ, guard.label));
        edge.add(new VarInsnNode(Opcodes.ALOAD, temporary));
        method.instructions.insert(call, edge);
    }

    private static AbstractInsnNode executable(AbstractInsnNode instruction) {
        while (instruction != null && instruction.getOpcode() < 0) {
            instruction = instruction.getNext();
        }
        return instruction;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
