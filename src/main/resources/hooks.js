function initializeCoreMod() {
    return {
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