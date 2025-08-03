package uz.alex2276564.smartspawnpoint.commands.framework.builder;

public interface NestedSubCommandProvider {
    SubCommandBuilder build(SubCommandBuilder parent);
}