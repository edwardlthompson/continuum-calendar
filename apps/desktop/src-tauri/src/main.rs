// GUI subsystem always — Explorer / login must not open a console (KB-035).
#![windows_subsystem = "windows"]

fn main() {
    app_lib::run();
}
