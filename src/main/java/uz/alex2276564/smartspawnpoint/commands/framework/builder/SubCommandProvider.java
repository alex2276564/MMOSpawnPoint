package uz.alex2276564.smartspawnpoint.commands.framework.builder;

public interface SubCommandProvider {
    SubCommandBuilder build(CommandBuilder parent);
}