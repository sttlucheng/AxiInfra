task("rtl", function()
  set_menu {
    usage = "xmake rtl [options]",
    description = "Generate RTL",
    options = {
        {'b', "build-dir", "kv", "build", "build directory"},
    }
  }
  on_run(function()
    import("core.base.option")
    local build_dir = option.get("build-dir")
    local rtl_dir = path.join(build_dir, "rtl")

    local chisel_opts = {"-i", "lmss.runMain", "lmss.generator.LmssGenerator"}
    table.join2(chisel_opts, {"--throw-on-first-error", "--target", "systemverilog", "--split-verilog", "--full-stacktrace", "-td", rtl_dir})

    if os.exists(rtl_dir) then os.rmdir(rtl_dir) end
    
    if os.host() == "windows" then
      os.execv(os.shell(), table.join({"mill"}, chisel_opts))
    else
      os.execv("mill", chisel_opts)
    end
    os.rm(path.join(rtl_dir, "firrtl_black_box_resource_files.f"))
    os.rm(path.join(rtl_dir, "filelist.f"))
    os.rm(path.join(rtl_dir, "extern_modules.sv"))
  end)
end)

task("init", function()
    on_run(function()
        os.cd(os.scriptdir())
        os.exec("git submodule update --init")
        os.cd(path.join(os.scriptdir(), "rocket-chip", "dependencies"))
        os.exec("git submodule update --init cde hardfloat diplomacy")
    end)
    set_menu {
        options = {} -- If no options required, just set it to {} and DO NOT remove this line. (`options` key is required)
    }
end)

task("comp", function()
    on_run(function()
        if os.host() == "windows" then
            os.execv(os.shell(), { "mill", "-i", "lmss.compile" })
        else
            os.execv("mill", { "-i", "lmss.compile" })
        end
    end)
    set_menu {
        options = {} -- If no options required, just set it to {} and DO NOT remove this line. (`options` key is required)
    }
end)

task("idea", function()
    on_run(function()
        if os.host() == "windows" then
            os.execv(os.shell(), { "mill", "-i", "mill.idea.GenIdea/idea" })
        else
            os.execv("mill", { "-i", "mill.idea.GenIdea/idea" })
        end
    end)
    set_menu {
        options = {}
    }
end)