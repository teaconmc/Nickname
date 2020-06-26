function initializeCoreMod() {
    return {
        "tab_display_name": {
            "target": {
                "type": "METHOD",
                "class": "net.minecraft.entity.player.ServerPlayerEntity",
                "methodName": "func_175396_E", // getTabListDisplayName
                "methodDesc": "()Lnet/minecraft/util/text/ITextComponent;"
            },
            "transformer": function (method) {
                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

                method.instructions.clear();
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                method.instructions.add(ASMAPI.buildMethodCall(
                    "net/minecraft/entity/player/PlayerEntity",
                    ASMAPI.mapMethod("func_145748_c_"), // getDisplayName
                    "()Lnet/minecraft/util/text/ITextComponent;",
                    ASMAPI.MethodType.VIRTUAL
                ));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));

                return method;
            }
        },
        "get_display_name": {
            "target": {
                "type": "METHOD",
                "class": "net.minecraft.entity.player.PlayerEntity",
                "methodName": "func_145748_c_", // getDisplayName
                "methodDesc": "()Lnet/minecraft/util/text/ITextComponent;"
            },
            "transformer": function (method) {
                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');

                method.instructions.set(ASMAPI.findFirstMethodCall(
                    method, 
                    ASMAPI.MethodType.VIRTUAL,
                    "net/minecraft/entity/player/PlayerEntity",
                    ASMAPI.mapMethod("func_200200_C_"), // getName
                    "()Lnet/minecraft/util/text/ITextComponent;"
                ), ASMAPI.buildMethodCall(
                    "org/teacon/nickname/Hooks",
                    "getPlayerBaseName",
                    "(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/util/text/ITextComponent;",
                    ASMAPI.MethodType.STATIC
                ));
                
                return method;
            }
        }
    }
}