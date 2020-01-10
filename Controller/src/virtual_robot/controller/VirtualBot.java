package virtual_robot.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import com.qualcomm.robotcore.hardware.HardwareMap;
import virtual_robot.config.Config;

/**
 *   For internal use only. Abstract base class for all of the specific robot configurations.
 *
 *   A robot config class that extend VirtualBot must:
 *
 *   1) Provide a no-argument init() method whose first statement is super.init();
 *   2) Provide a setupDisplayGroup() method that returns a JavaFX Group object (the graphical representation
 *          of the robot).
 *   3) Provide a createHardwareMap() method;
 *   4) Provide a public synchronized updateStateAndSensors(double millis) method;
 *   5) Provide a public powerDownAndReset() method.
 *
 *   Optionally (and in most cases), it will also be necessary to:
 *
 *   Override the public synchronized updateDisplay() method to update the appearance of accessories.
 *   This override should have super.updateDisplay() as its first statement.
 *
 */
public abstract class VirtualBot {

    protected static VirtualRobotController controller;

    protected HardwareMap hardwareMap;

    protected Group displayGroup = null;

    protected Group subSceneGroup;
    protected double fieldWidth;
    protected double halfFieldWidth;
    protected double halfBotWidth;
    protected double botWidth;

    protected double x = 0;
    protected double y = 0;
    protected double headingRadians = 0;

    public VirtualBot(){
        subSceneGroup = controller.getSubSceneGroup();
        this.fieldWidth = VirtualRobotController.FIELD_WIDTH;
        halfFieldWidth = fieldWidth / 2.0;
        botWidth = fieldWidth / 8.0;
        halfBotWidth = botWidth / 2.0;
    }

    public void init(){
        createHardwareMap();
        setUpDisplayGroup();
    }

    static void setController(VirtualRobotController ctrl){
        controller = ctrl;
    }

    /**
     * Get the display group from the concrete robot class
     */
    protected abstract Group getDisplayGroup();

    /**
     * Set up the Group object that will be displayed as the virtual robot. The resource file should contain
     * a Group with a 75x75 rectangle (The chassis rectangle) as its lowest layer, and other robot components
     * on top of that rectangle.
     *
     */
    protected void setUpDisplayGroup(){

        displayGroup = getDisplayGroup();

        /*
          Add transforms. They will be applied in the opposite order from the order in which they are added.
          The scale transform scales the entire display group so that the base layer has the same width as the field,
          and the chassis rectangle (originally the 75x75 rectangle) is one-eight of the field width.
          The rotate and translate transforms are added so that they can be manipulated later, when the robot moves
          around the field.
         */
        displayGroup.getTransforms().add(new Translate(0, 0));
        displayGroup.getTransforms().add(new Rotate(0, 0, 0));

        subSceneGroup.getChildren().add(displayGroup);
    }

    /**
     *  Update the state of the robot. This includes the x, y, and headingRadians variables, as well other variables
     *  that may need to be updated for a specific robot configuration.
     *
     *  Also, update the robot's sensors by calling the update.. methods of the sensors (e.g., the
     *  updateDistance(...) method of the distance sensors).
     *
     *  updateStateAndSensors is called on a non-UI thread via an ExecutorService object. For that reason,
     *  it SHOULD NOT make changes to the robot's graphical UI. Those changes should be made by
     *  overriding the updateDisplay() method, which is run on the UI thread.
     *
     *  @param millis milliseconds since the previous update
     */
    public abstract void updateStateAndSensors(double millis);

    /**
     *  Update the display based on the current x, y, and headingRadians of the robot.
     *  This method is run on the UI thread via a call to Platform.runLater(...).
     *
     *  For most robot configurations, it will be necessary to override this method, so as to
     *  implement graphical behavior that is specific to an individual robot configuration.
     *
     *  When overriding updateDisplay(), the first statement of the override method should
     *  be: super.updateDisplay().
     *
     */
    public synchronized void updateDisplay(){
        double displayX = x;
        double displayY = y;
        double displayAngle = headingRadians * 180.0 / Math.PI;
        Translate translate = (Translate)displayGroup.getTransforms().get(0);
        translate.setX(displayX);
        translate.setY(displayY);
        ((Rotate)displayGroup.getTransforms().get(1)).setAngle(displayAngle);
    }

    /**
     * Stop all motors; De-initialize or close other hardware (e.g. gyro/IMU) as appropriate.
     */
    public abstract void powerDownAndReset();

    public double getHeadingRadians(){ return headingRadians; }

    public void positionWithMouseClick(MouseEvent arg){
        if (arg.getButton() == MouseButton.PRIMARY) {
            double argX = Math.min(halfFieldWidth-halfBotWidth,
                    Math.max((arg.getX()- Config.SUBSCENE_WIDTH/2.0)*fieldWidth/Config.SUBSCENE_WIDTH, -(halfFieldWidth-halfBotWidth)));
            double argY = Math.min(halfFieldWidth-halfBotWidth,
                    Math.max(-(arg.getY()- Config.SUBSCENE_WIDTH/2.0)*fieldWidth/Config.SUBSCENE_WIDTH, -(halfFieldWidth-halfBotWidth)));
            x = argX;
            y = argY;
            updateDisplay();
        }
        else if (arg.getButton() == MouseButton.SECONDARY){
            double clickX = (arg.getX() - Config.SUBSCENE_WIDTH/2.0) * fieldWidth/Config.SUBSCENE_WIDTH;
            double clickY = (Config.SUBSCENE_WIDTH/2.0 - arg.getY()) * fieldWidth/Config.SUBSCENE_WIDTH;
            double radians = Math.atan2(clickY - y, clickX - x) - Math.PI/2.0;
            if (radians > Math.PI) radians -= 2.0*Math.PI;
            else if (radians < -Math.PI) radians += 2.0 * Math.PI;
            headingRadians = radians;
            updateDisplay();
        }
    }

    public void removeFromDisplay(){
        subSceneGroup.getChildren().remove(displayGroup);
    }

    public HardwareMap getHardwareMap(){ return hardwareMap; }

    /**
     * Create the HardwareMap object for the specific robot configuration, and assign it to the
     * hardwareMap variable.
     */
    protected abstract void createHardwareMap();

}
