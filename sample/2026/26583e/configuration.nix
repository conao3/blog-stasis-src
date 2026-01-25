{ modulesPath, pkgs, ... }: {
  imports = [ (modulesPath + "/virtualisation/qemu-vm.nix") ];

  system.stateVersion = "24.11";

  users.users.demo = {
    isNormalUser = true;
    extraGroups = [ "wheel" ];
    initialPassword = "demo";
  };

  services.xserver.enable = true;
  services.xserver.desktopManager.xfce.enable = true;
  services.xserver.desktopManager.xfce.enableScreensaver = false;

  environment.systemPackages = with pkgs; [ git ];

  services.emacs = {
    enable = true;
    defaultEditor = true;
  };

  environment.shellAliases = {
    e = "$EDITOR";
  };
}
