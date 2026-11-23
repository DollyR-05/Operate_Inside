package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.util.Stack;
import java.util.Arrays;

public class CalculatorController {

    @FXML
    private TextField display;

    private StringBuilder currentInput = new StringBuilder();
    // Stack<Object> allows storing both Double (numbers) and String (operators)
    private Stack<Object> stack = new Stack<>();

    @FXML
    private void onNumber(javafx.event.ActionEvent event) {
        String value = ((javafx.scene.control.Button) event.getSource()).getText();
        currentInput.append(value);
        display.setText(currentInput.toString());
    }

    @FXML
    private void onOperator(javafx.event.ActionEvent event) {
        String op = ((javafx.scene.control.Button) event.getSource()).getText();

        // 1. Push the current number input before the operator
        pushCurrentInput();

        // 2. Store the operator as a String
        stack.push(op);

        // 3. Update display to show the expression being built
        display.setText(display.getText() + " " + op + " ");
    }

    @FXML
    private void onEvaluate() {
        // Push the final input number before evaluating
        pushCurrentInput();

        // Evaluate the expression
        double result = evaluateStack();

        // Reset state
        currentInput.setLength(0);
        stack.clear();

        // Push the result back onto the stack to allow for chained calculations
        stack.push(result);

        display.setText(String.valueOf(result));
    }

    @FXML
    private void onClear() {
        currentInput.setLength(0);
        stack.clear();
        display.clear();
    }

    private void pushCurrentInput() {
        if (currentInput.length() > 0) {
            try {
                // Push the current number onto the stack
                stack.push(Double.parseDouble(currentInput.toString()));
                currentInput.setLength(0);
                display.setText(currentInput.toString()); // Clear number from input line
            } catch (NumberFormatException e) {
                display.setText("Error: Invalid Number Format");
            }
        }
    }

    /**
     * Evaluates the expression stored in the stack using left-to-right processing.
     */
    private double evaluateStack() {
        if (stack.isEmpty()) return 0;

        // Get elements in insertion order (left-to-right) for simple algebraic evaluation
        Object[] expression = stack.toArray();

        double result = 0;
        Character pendingOperator = null;

        for (Object element : expression) {
            if (element instanceof Double) {
                double number = (Double) element;

                if (pendingOperator == null) {
                    // Initialize result with the first number
                    result = number;
                } else {
                    // Perform calculation
                    switch (pendingOperator) {
                        case '+': result += number; break;
                        case '-': result -= number; break;
                        case '*': result *= number; break;
                        case '/':
                            if (number != 0) {
                                result /= number;
                            } else {
                                display.setText("Error: Division by zero");
                                return Double.NaN;
                            }
                            break;
                        default: break;
                    }
                    pendingOperator = null; // Clear operator after use
                }
            } else if (element instanceof String) {
                // Store the operator for the next calculation
                pendingOperator = ((String) element).charAt(0);
            }
        }

        return result;
    }
}