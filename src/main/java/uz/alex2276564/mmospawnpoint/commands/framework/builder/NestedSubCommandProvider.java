package uz.alex2276564.mmospawnpoint.commands.framework.builder;

public interface NestedSubCommandProvider {
    SubCommandBuilder build(SubCommandBuilder parent);
}